package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CommonStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final JwtCookieManager jwtCookieManager;

    @Override
    public AuthResponse issue(Member member) {
        String subject = String.valueOf(member.getId());
        String accessToken = jwtProvider.createAccessToken(subject, member.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(subject);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.of(member.getId(), refreshToken, expiresAt));
        return AuthResponse.tokens(accessToken, refreshToken, jwtProvider.getAccessTokenValiditySeconds());
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
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

    // 1. 헤더 -> 쿠키 순으로 access token 조회  2. 없거나 유효하지 않으면 401
    // 3. 토큰의 회원 조회  4. 토큰 쿠키 만료  5. 회원의 refresh token 전부 삭제(모든 기기 로그아웃)
    @Override
    public AuthResponse deleteTokens(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtCookieManager.resolveAccessToken(request)
                .filter(jwtProvider::validateAccessToken)
                .orElseThrow(() -> new CustomException(CommonStatusCode.INVALID_TOKEN));
        Member member = memberService.getById(Long.valueOf(jwtProvider.getSubject(accessToken)));

        jwtCookieManager.expireTokenCookies(response);
        refreshTokenRepository.deleteAllByMemberId(member.getId());
        return AuthResponse.message(AuthStatusCode.SIGNOUT_SUCCESS.getMessage());
    }

    @EventListener
    public void revokeAll(MemberWithdrawnEvent event) {
        refreshTokenRepository.deleteAllByMemberId(event.memberId());
    }
}
