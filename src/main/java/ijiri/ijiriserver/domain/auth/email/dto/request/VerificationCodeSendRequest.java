package ijiri.ijiriserver.domain.auth.email.dto.request;

import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record VerificationCodeSendRequest(
        @Schema(description = "인증할 이메일", example = "user@ijiri.com")
        @NotBlank @Email String email,

        @Schema(description = "용도. 비우면 SIGNUP", example = "SIGNUP")
        VerificationPurpose purpose
) {

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    public VerificationCodeSendRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        purpose = purpose == null ? VerificationPurpose.SIGNUP : purpose;
    }
}
