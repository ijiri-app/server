package ijiri.ijiriserver.common.exception

import org.springframework.http.HttpStatus

/**
 * 다른 서비스가 돌려준 오류를 그대로 전달하기 위한 상태 코드. 내부 API 도 BaseResponse 로 응답하므로
 * HTTP 상태, code, message 를 옮겨 담으면 호출한 쪽이 같은 오류로 응답할 수 있다 (예: INVALID_TRIM, INVALID_IMAGE).
 */
data class RemoteStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode
