package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    // 로그인한 기기 수 상한. 넘으면 가장 오래된 로그인부터 끊는다
    private static final int MAX_TOKENS_PER_MEMBER = 10;

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    @Override
    @Transactional
    public AuthResponse issue(Member member) {
        String subject = String.valueOf(member.getId());
        String accessToken = jwtProvider.createAccessToken(subject, member.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(subject);

        LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.of(member.getId(), refreshToken, expiresAt));
        removeOldestBeyondLimit(member.getId());
        return AuthResponse.tokens(accessToken, refreshToken, jwtProvider.getAccessTokenValiditySeconds());
    }

    // 서명이 유효한 refresh token 이 DB 에 없다면 이미 교체(rotation)됐거나 로그아웃된 토큰이다.
    // 탈취된 토큰의 재사용일 수 있으므로 그 회원의 모든 refresh token 을 폐기한다.
    // 이 폐기가 롤백되지 않도록 CustomException 에도 커밋한다
    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        Long memberId = Long.valueOf(jwtProvider.getSubject(refreshToken));
        int deleted = refreshTokenRepository.deleteValidByTokenHash(
                RefreshToken.hash(refreshToken),
                LocalDateTime.now(clock)
        );
        if (deleted == 0) {
            log.warn("Refresh token reuse detected: memberId={}", memberId);
            refreshTokenRepository.deleteAllByMemberId(memberId);
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        return issue(memberService.getById(memberId));
    }

    // 모든 기기에서 로그아웃. access token 은 stateless 라 만료(최대 1시간)까지 유효하다
    @Override
    @Transactional
    public AuthResponse signOut(Long memberId) {
        refreshTokenRepository.deleteAllByMemberId(memberId);
        return AuthResponse.message(AuthStatusCode.SIGNOUT_SUCCESS.getMessage());
    }

    @EventListener
    @Transactional
    public void revokeAll(MemberWithdrawnEvent event) {
        refreshTokenRepository.deleteAllByMemberId(event.memberId());
    }

    private void removeOldestBeyondLimit(Long memberId) {
        List<Long> ids = refreshTokenRepository.findIdsByMemberIdNewestFirst(memberId);
        if (ids.size() > MAX_TOKENS_PER_MEMBER) {
            refreshTokenRepository.deleteAllByIdIn(ids.subList(MAX_TOKENS_PER_MEMBER, ids.size()));
        }
    }
}
