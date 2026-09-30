package ijiri.ijiriserver.domain.auth.email.dto.request;

import ijiri.ijiriserver.global.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record SignInRequest(
        @Schema(description = "이메일", example = "user@ijiri.com")
        @NotBlank @Email String email,

        // BCrypt 는 72바이트까지만 처리하므로 그 이상은 비교 전에 거른다
        @Schema(description = "비밀번호")
        @NotBlank @MaxUtf8Bytes(72) String password
) {

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    public SignInRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
