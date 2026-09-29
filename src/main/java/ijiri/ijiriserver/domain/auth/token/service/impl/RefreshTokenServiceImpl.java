package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.auth.token.service.LogoutService;
import ijiri.ijiriserver.domain.auth.token.service.TokenIssueService;
import ijiri.ijiriserver.domain.auth.token.service.TokenReissueService;
import ijiri.ijiriserver.domain.auth.token.service.TokenRevokeService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl
        implements TokenIssueService, TokenReissueService, LogoutService, TokenRevokeService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    @Override
    public TokenResponse issue(Member member) {
        String subject = String.valueOf(member.getId());
        String accessToken = jwtProvider.createAccessToken(subject, member.getRole().getKey());
        String refreshToken = jwtProvider.createRefreshToken(subject);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());
        refreshTokenRepository.save(new RefreshToken(member.getId(), refreshToken, expiresAt));
        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    public TokenResponse reissue(String refreshToken) {
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

        Member member = memberRepository.findById(saved.getMemberId())
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
        return issue(member);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByTokenHash(RefreshToken.hash(refreshToken));
    }

    @Override
    public void revokeAll(Long memberId) {
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }
}
