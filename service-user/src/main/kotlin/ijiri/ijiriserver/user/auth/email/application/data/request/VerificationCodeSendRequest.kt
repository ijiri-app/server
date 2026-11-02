package ijiri.ijiriserver.user.auth.email.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.Locale

/**
 * 가입용 인증 코드 발송과 비밀번호 재설정 코드 발송에 함께 쓴다.
 */
class VerificationCodeSendRequest(
    email: String?,
) {

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    @Schema(description = "이메일", example = "user@ijiri.com")
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String? = email?.trim()?.lowercase(Locale.ROOT)
}
