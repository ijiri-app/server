package ijiri.ijiriserver.domain.auth.common.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthStatusCode implements StatusCode {

    LOGIN_SUCCESS(HttpStatus.OK, "AUTH200", "로그인에 성공했습니다."),
    REISSUE_SUCCESS(HttpStatus.OK, "AUTH2001", "토큰이 재발급되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH2002", "로그아웃되었습니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4011", "유효하지 않은 소셜 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4012", "유효하지 않은 리프레시 토큰입니다."),
    SOCIAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH502", "소셜 로그인 서버와 통신에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
