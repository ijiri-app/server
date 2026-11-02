package ijiri.ijiriserver.global.exception.exception

import ijiri.ijiriserver.global.exception.status_code.StatusCode

class CustomException(
    val statusCode: StatusCode,
    // 오류 응답 result 에 그대로 실린다 (retryAfterSeconds, remainingAttempts 등). 없으면 null
    val details: Map<String, Any?>? = null,
) : RuntimeException(statusCode.message)
