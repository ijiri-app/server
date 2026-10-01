package ijiri.ijiriserver.domain.auth.email.dto.request;

import ijiri.ijiriserver.global.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * RESET_PASSWORD 용도로 인증 코드 확인을 마친 이메일만 재설정할 수 있다.
 */
public record PasswordResetRequest(
        @Schema(description = "인증을 마친 이메일", example = "user@ijiri.com")
        @NotBlank @Email String email,

        @Schema(description = "새 비밀번호 (8~20자, 영문과 숫자 각 1개 이상)")
        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(regexp = SignupRequest.PASSWORD_REGEX, message = "영문과 숫자를 각각 1개 이상 포함해야 합니다.")
        @MaxUtf8Bytes(72)
        String newPassword
) {

    public PasswordResetRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
