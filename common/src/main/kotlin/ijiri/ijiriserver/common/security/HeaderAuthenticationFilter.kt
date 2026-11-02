package ijiri.ijiriserver.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 게이트웨이가 넣은 X-Member-Id / X-Member-Role 로 인증 정보를 만든다. 헤더가 없으면 비로그인 요청이다
 * (익명 인증을 쓰지 않으므로 @AuthenticationPrincipal 은 null).
 */
@Component
class HeaderAuthenticationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val memberId: String? = request.getHeader(GatewayHeaders.MEMBER_ID)
        if (StringUtils.hasText(memberId)) {
            val role = request.getHeader(GatewayHeaders.MEMBER_ROLE)?.takeIf { StringUtils.hasText(it) } ?: DEFAULT_ROLE
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                memberId,
                null,
                listOf(SimpleGrantedAuthority(ROLE_PREFIX + role)),
            )
        }
        chain.doFilter(request, response)
    }

    companion object {
        private const val DEFAULT_ROLE = "USER"

        // Spring Security 의 hasRole() 은 "ROLE_" 접두사가 붙은 authority 를 기대한다
        private const val ROLE_PREFIX = "ROLE_"
    }
}
