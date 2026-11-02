package ijiri.ijiriserver.domain.post.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class PostImageRequest(
    @Schema(description = "POST /uploads/images 로 받아 올린 imageKey", example = "posts/tmp/ab12.jpg")
    @field:NotBlank @field:Size(max = 100)
    val imageKey: String?,

    @Schema(description = "올린 사진의 가로 픽셀 (수정할 때는 무시)", example = "1536")
    @field:Positive
    val width: Int?,

    @Schema(description = "올린 사진의 세로 픽셀 (수정할 때는 무시)", example = "2048")
    @field:Positive
    val height: Int?,

    // 보내지 않거나 null 이면 태그 없음 (읽을 때 orEmpty())
    @Schema(description = "이 사진의 부품 태그 (최대 30개)")
    @field:Size(max = 30)
    val tags: List<@NotNull @Valid PostTagRequest>?,
)
