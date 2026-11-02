package ijiri.ijiriserver.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import ijiri.ijiriserver.common.exception.CommonStatusCode
import ijiri.ijiriserver.common.exception.StatusCode

@JsonPropertyOrder("code", "message", "result")
data class BaseResponse<T>(
    val code: String,
    val message: String,
    @JsonInclude(JsonInclude.Include.NON_NULL) val result: T?,
) {

    companion object {

        fun <T> ok(result: T): BaseResponse<T> = of(CommonStatusCode.OK, result)

        fun <T> of(statusCode: StatusCode, result: T?): BaseResponse<T> =
            BaseResponse(statusCode.code, statusCode.message, result)

        // 에러 응답임을 호출부에서 드러내기 위한 이름. 형태는 성공 응답과 같다
        fun <T> onFailure(statusCode: StatusCode, result: T?): BaseResponse<T> = of(statusCode, result)

        fun <T> onFailure(statusCode: StatusCode): BaseResponse<T> = onFailure(statusCode, null)
    }
}
