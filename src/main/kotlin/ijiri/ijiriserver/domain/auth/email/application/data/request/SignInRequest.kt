package ijiri.ijiriserver.domain.auth.email.application.data.request

import ijiri.ijiriserver.global.validation.MaxUtf8Bytes
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.Locale

class SignInRequest(
    email: String?,

    // BCrypt 는 72바이트까지만 처리하므로 그 이상은 비교 전에 거른다
    @Schema(description = "비밀번호")
    @field:NotBlank @field:MaxUtf8Bytes(72) val password: String?,
) {

    // 대소문자/공백만 다른 이메일로 중복 가입되지 않도록 소문자로 통일
    @Schema(description = "이메일", example = "user@ijiri.com")
    @field:NotBlank
    @field:Email
    val email: String? = email?.trim()?.lowercase(Locale.ROOT)
}
