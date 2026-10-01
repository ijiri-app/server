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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            description = "숫자 6자리 코드를 발송. 5분간 유효. 재발송은 60초 후, IP 당 시간당 10회. "
                    + "purpose = SIGNUP(기본) / RESET_PASSWORD. 가입 여부와 무관하게 같은 응답을 준다"
    )
    @PostMapping("/verification-code")
    public BaseResponse<AuthResponse> sendCode(
            @Valid @RequestBody VerificationCodeSendRequest request,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.check(SEND_KEY_PREFIX + httpRequest.getRemoteAddr(), SEND_LIMIT_PER_IP, SEND_LIMIT_PERIOD);
        return BaseResponse.of(
                AuthStatusCode.VERIFICATION_CODE_SENT,
                emailVerificationService.sendCode(request.email(), request.purpose())
        );
    }

    @Operation(
            summary = "인증 코드 확인",
            description = "코드가 맞으면 이메일 인증 완료. 이후 30분 안에 회원가입(비밀번호 재설정)해야 한다. "
                    + "코드당 5회까지 시도"
    )
    @PostMapping("/verification-code/verify")
    public BaseResponse<AuthResponse> verifyCode(@Valid @RequestBody VerificationCodeConfirmRequest request) {
        return BaseResponse.of(
                AuthStatusCode.EMAIL_VERIFIED,
                emailVerificationService.verifyCode(request.email(), request.code(), request.purpose())
        );
    }
}
