package ijiri.ijiriserver.domain.auth.email.dto.request;

import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Locale;

public record VerificationCodeConfirmRequest(
        @Schema(description = "인증 코드를 받은 이메일", example = "user@ijiri.com")
        @NotBlank @Email String email,

        @Schema(description = "메일로 받은 숫자 6자리 인증 코드", example = "123456")
        @NotBlank @Pattern(regexp = "\\d{6}") String code,

        @Schema(description = "발송 요청의 용도 (선택). 비우면 이 이메일로 가장 최근에 보낸 코드와 비교", example = "SIGNUP")
        VerificationPurpose purpose
) {

    // 발송 요청과 같은 형태로 비교되도록 소문자로 통일
    public VerificationCodeConfirmRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
