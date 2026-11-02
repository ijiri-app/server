package ijiri.ijiriserver.global.jwt

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.time.Duration

/**
 * access/refresh token 쿠키 발급, 만료, 조회.
 * 토큰은 Authorization 헤더를 먼저 보고, 없으면 쿠키를 본다.
 */
@Component
class JwtCookieManager(
    private val jwtProvider: JwtProvider,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: List<String>,
) {

    fun addTokenCookies(response: HttpServletResponse, accessToken: String, refreshToken: String) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, jwtProvider.getAccessTokenValiditySeconds())
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, jwtProvider.getRefreshTokenValiditySeconds())
    }

    fun expireTokenCookies(response: HttpServletResponse) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0)
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0)
    }

    fun resolveAccessToken(request: HttpServletRequest): String? {
        val header: String? = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length)
        }
        return getCookie(request, ACCESS_TOKEN_COOKIE)
    }

    // 앱은 body 로, 웹은 쿠키로 보낸다
    fun resolveRefreshToken(request: HttpServletRequest, bodyToken: String?): String? {
        if (StringUtils.hasText(bodyToken)) {
            return bodyToken
        }
        return getCookie(request, REFRESH_TOKEN_COOKIE)
    }

    // 프론트가 다른 도메인이어도 쿠키가 전송되도록 SameSite=None + Secure. JS 에서 읽지 못하게 HttpOnly
    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAgeSeconds: Long) {
        val cookie = ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    // CSRF 방어: SameSite=None 쿠키는 다른 사이트의 요청에도 실리므로,
    // 허용되지 않은 Origin 에서 온 요청이면 쿠키 토큰을 무시한다 (Origin 이 없는 앱/같은 출처 요청은 통과)
    private fun getCookie(request: HttpServletRequest, name: String): String? {
        val cookies = request.cookies
        if (cookies == null || !isTrustedOrigin(request)) {
            return null
        }
        return cookies
            .filter { name == it.name }
            .map { it.value }
            .firstOrNull { StringUtils.hasText(it) }
    }

    private fun isTrustedOrigin(request: HttpServletRequest): Boolean {
        val origin: String? = request.getHeader(HttpHeaders.ORIGIN)
        return origin == null || allowedOrigins.contains(origin)
    }

    companion object {
        const val ACCESS_TOKEN_COOKIE = "accessToken"
        const val REFRESH_TOKEN_COOKIE = "refreshToken"
        private const val BEARER_PREFIX = "Bearer "
    }
}
