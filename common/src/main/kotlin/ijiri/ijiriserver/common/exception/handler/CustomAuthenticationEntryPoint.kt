package ijiri.ijiriserver.common.exception.handler

import ijiri.ijiriserver.common.exception.CommonStatusCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val baseResponseWriter: BaseResponseWriter,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        baseResponseWriter.writeFailure(response, CommonStatusCode.UNAUTHORIZED)
    }
}
