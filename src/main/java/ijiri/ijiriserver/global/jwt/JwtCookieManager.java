package ijiri.ijiriserver.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * access/refresh token 쿠키 발급, 만료, 조회.
 * 토큰은 Authorization 헤더를 먼저 보고, 없으면 쿠키를 본다.
 */
@Component
public class JwtCookieManager {

    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final List<String> allowedOrigins;

    public JwtCookieManager(
            JwtProvider jwtProvider,
            @Value("${cors.allowed-origins}") List<String> allowedOrigins
    ) {
        this.jwtProvider = jwtProvider;
        this.allowedOrigins = allowedOrigins;
    }

    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, jwtProvider.getAccessTokenValiditySeconds());
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, jwtProvider.getRefreshTokenValiditySeconds());
    }

    public void expireTokenCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    public Optional<String> resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return getCookie(request, ACCESS_TOKEN_COOKIE);
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        return getCookie(request, REFRESH_TOKEN_COOKIE);
    }

    // 프론트가 다른 도메인이어도 쿠키가 전송되도록 SameSite=None + Secure. JS 에서 읽지 못하게 HttpOnly
    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // CSRF 방어: SameSite=None 쿠키는 다른 사이트의 요청에도 실리므로,
    // 허용되지 않은 Origin 에서 온 요청이면 쿠키 토큰을 무시한다 (Origin 이 없는 앱/같은 출처 요청은 통과)
    private Optional<String> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null || !isTrustedOrigin(request)) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private boolean isTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        return origin == null || allowedOrigins.contains(origin);
    }
}
