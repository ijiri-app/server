package ijiri.ijiriserver.domain.auth.email.dto.request;

import ijiri.ijiriserver.global.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * 인증 코드 확인(/auth/email/verify-code)으로 받은 verificationToken 이 있어야 가입할 수 있다.
 */
public record SignupRequest(
        @Schema(description = "인증을 마친 이메일 (로그인 ID)", example = "user@ijiri.com")
        @NotBlank @Email String email,

        // BCrypt 는 72바이트까지만 처리하므로 글자 수와 별도로 바이트 길이도 제한한다 (한글은 1자 3바이트)
        @Schema(description = "비밀번호 (8~64자, 영문과 숫자 각 1개 이상)")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = PASSWORD_REGEX, message = "영문과 숫자를 각각 1개 이상 포함해야 합니다.")
        @MaxUtf8Bytes(72)
        String password,

        @Schema(description = "닉네임 (2~12자, 중복 불가)", example = "이지리")
        @NotBlank @Size(min = 2, max = 12) String nickname,

        @Schema(description = "인증 코드 확인 응답의 verificationToken")
        @NotBlank String verificationToken,

        @Schema(description = "필수 약관 동의 3개 (모두 true)")
        @NotNull @Valid Agreements agreements
) {

    // 비밀번호 재설정도 같은 규칙을 쓴다
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).*$";

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    public SignupRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        nickname = nickname == null ? null : nickname.trim();
    }

    public record Agreements(
            @Schema(description = "만 14세 이상") @AssertTrue boolean age14,
            @Schema(description = "이용약관") @AssertTrue boolean terms,
            @Schema(description = "개인정보 수집·이용") @AssertTrue boolean privacy
    ) {
    }
}
