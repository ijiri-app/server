package ijiri.ijiriserver.global.handler

import ijiri.ijiriserver.global.data.BaseResponse
import ijiri.ijiriserver.global.exception.status_code.CommonStatusCode
import ijiri.ijiriserver.global.exception.status_code.StatusCode
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 필터 단계 예외처럼 GlobalExceptionHandler 를 거치지 않고 /error 로 넘어온 요청도 BaseResponse 로 응답한다.
 * Spring 기본 에러 본문(timestamp, path 등)이 노출되지 않게 한다.
 */
@RestController
class CustomErrorController : ErrorController {

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): ResponseEntity<BaseResponse<Void>> {
        val statusCode = resolve(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
        return ResponseEntity.status(statusCode.httpStatus)
            .body(BaseResponse.onFailure(statusCode))
    }

    private fun resolve(status: Any?): StatusCode {
        val httpStatus = (status as? Int)?.let { HttpStatus.resolve(it) }
            ?: return CommonStatusCode.INTERNAL_SERVER_ERROR
        return CommonStatusCode.entries.firstOrNull { it.httpStatus == httpStatus }
            ?: if (httpStatus.is4xxClientError()) {
                CommonStatusCode.BAD_REQUEST
            } else {
                CommonStatusCode.INTERNAL_SERVER_ERROR
            }
    }
}
