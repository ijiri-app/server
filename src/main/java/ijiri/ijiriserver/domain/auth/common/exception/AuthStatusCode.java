package ijiri.ijiriserver.domain.auth.common.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthStatusCode implements StatusCode {

    SIGNIN_SUCCESS(HttpStatus.OK, "SIGNIN_SUCCESS", "로그인에 성공했습니다."),
    SIGNUP_SUCCESS(HttpStatus.OK, "SIGNUP_SUCCESS", "회원가입에 성공했습니다."),
    REFRESH_SUCCESS(HttpStatus.OK, "REFRESH_SUCCESS", "토큰이 재발급되었습니다."),
    EMAIL_VERIFIED(HttpStatus.OK, "EMAIL_VERIFIED", "이메일 인증이 완료되었습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER", "지원하지 않는 로그인 수단입니다."),
    // 틀림, 만료, 시도 횟수 초과를 모두 포함한다. result.remainingAttempts 로 남은 횟수를 준다
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "인증 코드가 올바르지 않습니다."),
    INVALID_VERIFICATION_TOKEN(
            HttpStatus.BAD_REQUEST,
            "INVALID_VERIFICATION_TOKEN",
            "이메일 인증이 만료되었거나 이미 사용되었습니다. 인증 코드를 다시 받아 주세요."
    ),
    INVALID_PROVIDER_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_PROVIDER_TOKEN", "유효하지 않은 소셜 토큰입니다."),
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_REFRESH_TOKEN",
            "리프레시 토큰이 없거나 유효하지 않습니다. 다시 로그인해 주세요."
    ),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "로그인에 5번 실패해 잠시 로그인할 수 없습니다."),
    VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "잠시 후 다시 요청해 주세요."),
    SOCIAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "SOCIAL_SERVER_ERROR", "소셜 로그인 서버와 통신에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "EMAIL_SEND_FAILED", "인증 메일 발송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
