package ijiri.ijiriserver.domain.auth.email.dto.request;

import ijiri.ijiriserver.global.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * POST /auth/password/reset/code 로 받은 코드로 바로 재설정한다.
 */
public record PasswordResetRequest(
        @Schema(description = "이메일", example = "user@ijiri.com")
        @NotBlank @Email String email,

        @Schema(description = "메일로 받은 숫자 6자리 코드", example = "123456")
        @NotBlank @Pattern(regexp = "\\d{6}") String code,

        @Schema(description = "새 비밀번호 (8~64자, 영문과 숫자 각 1개 이상)")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = SignupRequest.PASSWORD_REGEX, message = "영문과 숫자를 각각 1개 이상 포함해야 합니다.")
        @MaxUtf8Bytes(72)
        String newPassword
) {

    public PasswordResetRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
