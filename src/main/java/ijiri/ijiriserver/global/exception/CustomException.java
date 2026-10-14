package ijiri.ijiriserver.global.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class CustomException extends RuntimeException {

    private final StatusCode statusCode;
    // 오류 응답 result 에 그대로 실린다 (retryAfterSeconds, remainingAttempts 등). 없으면 null
    private final Map<String, Object> details;

    public CustomException(StatusCode statusCode) {
        this(statusCode, null);
    }

    public CustomException(StatusCode statusCode, Map<String, Object> details) {
        super(statusCode.getMessage());
        this.statusCode = statusCode;
        this.details = details;
    }
}
