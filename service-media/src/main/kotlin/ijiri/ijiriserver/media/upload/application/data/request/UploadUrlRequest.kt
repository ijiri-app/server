package ijiri.ijiriserver.media.upload.application.data.request

import ijiri.ijiriserver.common.contract.UploadPurpose
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

// purpose 를 생략하거나 null 로 보내면 POST 로 받으려고 data class 대신 일반 클래스로 둔다
class UploadUrlRequest(
    @Schema(description = "올릴 사진 수 (1~10)", example = "3")
    @field:NotNull @field:Min(1) @field:Max(10)
    val count: Int?,

    @Schema(description = "image/jpeg 또는 image/webp (앱이 변환해서 올린다)", example = "image/jpeg")
    @field:NotBlank @field:Pattern(regexp = "image/(jpeg|webp)")
    val contentType: String?,

    @Schema(description = "POST(게시물, 기본) / PROFILE(프로필 사진)", example = "POST")
    purpose: UploadPurpose?,
) {

    val purpose: UploadPurpose = purpose ?: UploadPurpose.POST
}
