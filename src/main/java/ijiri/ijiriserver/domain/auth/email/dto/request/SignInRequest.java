package ijiri.ijiriserver.domain.auth.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record SignInRequest(
        @Schema(description = "이메일", example = "user@ijiri.com")
        @NotBlank @Email String email,

        @Schema(description = "비밀번호")
        @NotBlank String password
) {

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    public SignInRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
