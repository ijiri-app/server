package ijiri.ijiriserver.common.exception.handler

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.common.exception.StatusCode
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Security 필터 단계는 GlobalExceptionHandler 를 거치지 않으므로 BaseResponse 를 직접 써서 응답한다.
 */
@Component
class BaseResponseWriter(
    private val objectMapper: ObjectMapper,
) {

    fun writeFailure(response: HttpServletResponse, statusCode: StatusCode) {
        response.status = statusCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        objectMapper.writeValue(response.writer, BaseResponse.onFailure<Void>(statusCode))
    }
}
