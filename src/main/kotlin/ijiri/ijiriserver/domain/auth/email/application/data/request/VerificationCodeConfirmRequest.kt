package ijiri.ijiriserver.domain.auth.email.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.Locale

class VerificationCodeConfirmRequest(
    email: String?,

    @Schema(description = "메일로 받은 숫자 6자리 인증 코드", example = "123456")
    @field:NotBlank @field:Pattern(regexp = "\\d{6}") val code: String?,
) {

    // 발송 요청과 같은 형태로 비교되도록 소문자로 통일
    @Schema(description = "인증 코드를 받은 이메일", example = "user@ijiri.com")
    @field:NotBlank
    @field:Email
    val email: String? = email?.trim()?.lowercase(Locale.ROOT)
}
