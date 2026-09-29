package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.VerificationCodeSendRequest;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
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
            description = "영문 + 숫자 6자리 코드를 발송. 10분간 유효하며 회원가입 요청에 함께 보낸다. 재발송은 60초 후"
    )
    @PostMapping("/verification-code")
    public BaseResponse<AuthResponse> sendCode(
            @Valid @RequestBody VerificationCodeSendRequest request
    ) {
        return BaseResponse.of(
                AuthStatusCode.VERIFICATION_CODE_SENT,
                emailVerificationService.sendCode(request.email())
        );
    }
}
