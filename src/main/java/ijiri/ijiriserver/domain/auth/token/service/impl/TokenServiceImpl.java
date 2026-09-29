package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @Override
    public TokenResponse issue(Member member) {
        String subject = String.valueOf(member.getId());
        String accessToken = jwtProvider.createAccessToken(subject, member.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(subject);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.of(member.getId(), refreshToken, expiresAt));
        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    public long getAccessTokenExpiresIn() {
        return jwtProvider.getAccessTokenValiditySeconds();
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        RefreshToken saved = refreshTokenRepository.findByTokenHash(RefreshToken.hash(refreshToken))
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));
        // 만료된 토큰은 여기서 지우지 않고 스케줄러가 정리
        if (saved.isExpired()) {
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        refreshTokenRepository.delete(saved);

        return issue(memberService.getById(saved.getMemberId()));
    }

    @Override
    public void signOut(Long memberId, String refreshToken) {
        refreshTokenRepository.deleteByTokenHashAndMemberId(RefreshToken.hash(refreshToken), memberId);
    }

    @EventListener
    public void revokeAll(MemberWithdrawnEvent event) {
        refreshTokenRepository.deleteAllByMemberId(event.memberId());
    }
}
