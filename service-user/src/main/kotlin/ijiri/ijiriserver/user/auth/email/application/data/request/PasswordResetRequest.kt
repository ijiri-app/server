package ijiri.ijiriserver.user.auth.email.application.data.request

import ijiri.ijiriserver.common.validation.MaxUtf8Bytes
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.Locale

/**
 * POST /auth/password/reset/code 로 받은 코드로 바로 재설정한다.
 */
class PasswordResetRequest(
    email: String?,

    @Schema(description = "메일로 받은 숫자 6자리 코드", example = "123456")
    @field:NotBlank @field:Pattern(regexp = "\\d{6}") val code: String?,

    @Schema(description = "새 비밀번호 (8~64자, 영문과 숫자 각 1개 이상)")
    @field:NotBlank
    @field:Size(min = 8, max = 64)
    @field:Pattern(regexp = SignupRequest.PASSWORD_REGEX, message = "영문과 숫자를 각각 1개 이상 포함해야 합니다.")
    @field:MaxUtf8Bytes(72)
    val newPassword: String?,
) {

    @Schema(description = "이메일", example = "user@ijiri.com")
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String? = email?.trim()?.lowercase(Locale.ROOT)
}
