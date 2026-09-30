package ijiri.ijiriserver.global.exception;

import ijiri.ijiriserver.global.response.BaseResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 필터 단계 예외처럼 GlobalExceptionHandler 를 거치지 않고 /error 로 넘어온 요청도 BaseResponse 로 응답한다.
 * Spring 기본 에러 본문(timestamp, path 등)이 노출되지 않게 한다.
 */
@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<BaseResponse<Void>> handleError(HttpServletRequest request) {
        StatusCode statusCode = resolve(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        return ResponseEntity.status(statusCode.getHttpStatus())
                .body(BaseResponse.onFailure(statusCode));
    }

    private StatusCode resolve(Object status) {
        HttpStatus httpStatus = status instanceof Integer code ? HttpStatus.resolve(code) : null;
        if (httpStatus == null) {
            return CommonStatusCode.INTERNAL_SERVER_ERROR;
        }
        return Arrays.stream(CommonStatusCode.values())
                .filter(code -> code.getHttpStatus() == httpStatus)
                .findFirst()
                .orElse(httpStatus.is4xxClientError()
                        ? CommonStatusCode.BAD_REQUEST
                        : CommonStatusCode.INTERNAL_SERVER_ERROR);
    }
}
