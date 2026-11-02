package ijiri.ijiriserver.global.jwt

import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val jwtCookieManager: JwtCookieManager,
    private val sessionValidator: SessionValidator,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val claims = jwtCookieManager.resolveAccessToken(request)
            ?.let { jwtProvider.parseAccessToken(it) }
            ?.takeIf { hasActiveSession(it) }
        if (claims != null) {
            SecurityContextHolder.getContext().authentication = jwtProvider.getAuthentication(claims)
        }
        chain.doFilter(request, response)
    }

    // 로그아웃/다른 기기 로그인/탈퇴로 세션이 끝났으면 만료 전 access token 도 인증하지 않는다
    private fun hasActiveSession(claims: Claims): Boolean {
        val sessionId = jwtProvider.getSessionId(claims)
        return sessionId != null && sessionValidator.isActive(sessionId)
    }
}
