package ijiri.ijiriserver.global.oauth2.handler;

import ijiri.ijiriserver.global.jwt.JwtProvider;
import ijiri.ijiriserver.global.oauth2.userinfo.OAuth2UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final JwtProvider jwtProvider;

    @Value("${oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(
                token.getAuthorizedClientRegistrationId(), token.getPrincipal().getAttributes());

        // TODO: User 엔티티 생기면 provider + providerId 로 조회/가입 후 userId 를 subject 로 사용
        String subject = userInfo.getProvider() + ":" + userInfo.getProviderId();
        String accessToken = jwtProvider.createAccessToken(subject, "ROLE_USER");
        String refreshToken = jwtProvider.createRefreshToken(subject);
        // TODO: refreshToken DB 저장 (재발급 시 대조, 로그아웃 시 삭제)
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie(refreshToken).toString());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(jwtProvider.getRefreshTokenValiditySeconds())
                .build();
    }
}
