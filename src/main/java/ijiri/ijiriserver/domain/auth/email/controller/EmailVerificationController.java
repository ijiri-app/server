package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.EmailVerifyRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.VerificationCodeSendRequest;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@SecurityRequirements
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "인증 코드 발송",
            description = "가입할 이메일로 영문 + 숫자 6자리 인증 코드를 발송. 코드는 10분간 유효, 재발송은 60초 후 가능"
    )
    @PostMapping("/verification-code")
    public BaseResponse<Void> sendCode(@Valid @RequestBody VerificationCodeSendRequest request) {
        emailVerificationService.sendCode(request.email());
        return BaseResponse.of(AuthStatusCode.VERIFICATION_CODE_SENT, null);
    }

    @Operation(
            summary = "인증 코드 확인",
            description = "인증에 성공하면 30분 안에 회원가입해야 한다. 5회 틀리면 코드를 다시 받아야 한다"
    )
    @PostMapping("/verify")
    public BaseResponse<Void> verify(@Valid @RequestBody EmailVerifyRequest request) {
        emailVerificationService.verify(request.email(), request.code());
        return BaseResponse.of(AuthStatusCode.EMAIL_VERIFIED, null);
    }
}
