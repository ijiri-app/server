package ijiri.ijiriserver.domain.auth.common.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthStatusCode implements StatusCode {

    SIGNIN_SUCCESS(HttpStatus.OK, "AUTH200", "로그인에 성공했습니다."),
    SIGNUP_SUCCESS(HttpStatus.CREATED, "AUTH201", "회원가입에 성공했습니다."),
    REFRESH_SUCCESS(HttpStatus.OK, "AUTH2001", "토큰이 재발급되었습니다."),
    SIGNOUT_SUCCESS(HttpStatus.OK, "AUTH2002", "로그아웃되었습니다."),
    VERIFICATION_CODE_SENT(HttpStatus.OK, "AUTH2003", "인증 코드가 발송되었습니다."),
    EMAIL_VERIFIED(HttpStatus.OK, "AUTH2004", "이메일 인증이 완료되었습니다."),
    PASSWORD_RESET(HttpStatus.OK, "AUTH2005", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH4001", "지원하지 않는 로그인 제공자입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH4003", "인증 코드가 올바르지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH4004", "인증 코드가 만료되었습니다. 다시 요청해 주세요."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH4005", "이메일 인증이 필요합니다. 인증 코드를 다시 받아 주세요."),
    INVALID_PROVIDER_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4011", "유효하지 않은 소셜 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4012", "리프레시 토큰이 없거나 유효하지 않습니다. 다시 로그인해 주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH4013", "이메일 또는 비밀번호가 올바르지 않습니다."),
    VERIFICATION_ATTEMPTS_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AUTH4291",
            "인증 시도 횟수를 초과했습니다. 코드를 다시 요청해 주세요."
    ),
    VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "AUTH4292", "잠시 후 다시 요청해 주세요."),
    SIGNIN_LOCKED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AUTH4293",
            "로그인에 5회 실패해 15분 동안 로그인할 수 없습니다. 비밀번호를 재설정하거나 잠시 후 다시 시도해 주세요."
    ),
    SOCIAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH502", "소셜 로그인 서버와 통신에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "AUTH5021", "인증 메일 발송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
