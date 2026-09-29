package ijiri.ijiriserver.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ijiri.ijiriserver.global.exception.CommonStatusCode;
import ijiri.ijiriserver.global.exception.StatusCode;

@JsonPropertyOrder({"code", "message", "result"})
public record BaseResponse<T>(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) T result
) {

    public static <T> BaseResponse<T> ok(T result) {
        return of(CommonStatusCode.OK, result);
    }

    public static <T> BaseResponse<T> of(StatusCode statusCode, T result) {
        return new BaseResponse<>(statusCode.getCode(), statusCode.getMessage(), result);
    }

    public static <T> BaseResponse<T> onFailure(StatusCode statusCode, T result) {
        return new BaseResponse<>(statusCode.getCode(), statusCode.getMessage(), result);
    }

    public static <T> BaseResponse<T> onFailure(StatusCode statusCode) {
        return onFailure(statusCode, null);
    }
}
