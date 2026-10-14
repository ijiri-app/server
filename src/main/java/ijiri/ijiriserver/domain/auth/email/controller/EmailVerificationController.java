package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.VerificationCodeConfirmRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.VerificationCodeSendRequest;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@SecurityRequirements
public class EmailVerificationController {

    // 이메일 주소를 바꿔가며 메일을 대량 발송시키는 것을 막는 IP 단위 제한
    private static final String SEND_KEY_PREFIX = "verification:ip:";
    private static final int SEND_LIMIT_PER_IP = 10;
    private static final Duration SEND_LIMIT_PERIOD = Duration.ofHours(1);

    private final EmailVerificationService emailVerificationService;
    private final RateLimiter rateLimiter;

    @Operation(
            summary = "인증 코드 발송",
            description = "가입용. 숫자 6자리, 5분 유효. 같은 이메일 1분에 1번, IP 당 시간당 10회(TOO_MANY_REQUESTS). 204. "
                    + "이미 가입된 이메일이어도 같은 응답을 주고 코드 대신 안내 메일을 보낸다"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/send-code")
    public void sendCode(
            @Valid @RequestBody VerificationCodeSendRequest request,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.check(SEND_KEY_PREFIX + httpRequest.getRemoteAddr(), SEND_LIMIT_PER_IP, SEND_LIMIT_PERIOD);
        emailVerificationService.sendSignupCode(request.email());
    }

    @Operation(
            summary = "인증 코드 확인",
            description = "가입용. 맞으면 verificationToken(10분, 1회용)을 준다. 틀리면 INVALID_VERIFICATION_CODE + "
                    + "remainingAttempts. 5번 틀리면 코드 무효"
    )
    @PostMapping("/verify-code")
    public BaseResponse<AuthResponse> verifyCode(@Valid @RequestBody VerificationCodeConfirmRequest request) {
        return BaseResponse.of(
                AuthStatusCode.EMAIL_VERIFIED,
                emailVerificationService.verifySignupCode(request.email(), request.code())
        );
    }
}
