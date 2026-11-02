package ijiri.ijiriserver.global.handler

import ijiri.ijiriserver.global.exception.status_code.CommonStatusCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(
    private val baseResponseWriter: BaseResponseWriter,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        baseResponseWriter.writeFailure(response, CommonStatusCode.FORBIDDEN)
    }
}
