package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    // 동시 접속 차단: 한 계정은 refresh token 을 하나만 가진다.
    // 새로 발급할 때 기존 토큰을 모두 지워, 다른 기기의 로그인은 다음 갱신 시점에 끊긴다
    @Override
    @Transactional
    public AuthResponse issue(Member member) {
        String subject = String.valueOf(member.getId());
        String accessToken = jwtProvider.createAccessToken(subject, member.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(subject);

        LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());
        refreshTokenRepository.deleteAllByMemberId(member.getId());
        refreshTokenRepository.save(RefreshToken.of(member.getId(), refreshToken, expiresAt));
        return AuthResponse.tokens(accessToken, refreshToken, jwtProvider.getAccessTokenValiditySeconds());
    }

    // 서명이 유효해도 DB 에 없으면 교체(rotation), 로그아웃, 또는 다른 기기 로그인으로 밀려난 토큰이다.
    // 다른 기기 로그인으로 밀려난 경우와 구분할 수 없으므로, 회원의 토큰을 모두 폐기하지 않고 거부만 한다.
    // (그렇게 하면 밀려난 기기의 갱신 시도가 방금 로그인한 기기까지 로그아웃시킨다)
    // 탈취된 토큰이 쓰이면 원래 기기의 갱신이 실패하고, 다시 로그인하는 순간 탈취한 쪽 토큰이 폐기된다
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        int deleted = refreshTokenRepository.deleteValidByTokenHash(
                RefreshToken.hash(refreshToken),
                LocalDateTime.now(clock)
        );
        if (deleted == 0) {
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        return issue(memberService.getById(Long.valueOf(jwtProvider.getSubject(refreshToken))));
    }
}
