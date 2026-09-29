package ijiri.ijiriserver.domain.auth.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SignupRequest(
        @Schema(description = "닉네임", example = "이지리오너")
        @NotBlank @Size(max = 100) String nickname,

        @Schema(description = "이메일 (로그인 ID)", example = "user@ijiri.com")
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8~64자)")
        @NotBlank @Size(min = 8, max = 64) String password,

        @Schema(description = "메일로 받은 6자리 인증 코드 (영문 + 숫자)", example = "A1B2C3")
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{6}") String verificationCode
) {

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    public SignupRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
