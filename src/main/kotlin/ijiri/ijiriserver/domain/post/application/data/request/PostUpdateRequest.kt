package ijiri.ijiriserver.domain.post.application.data.request

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로). 사진은 바꿀 수 없다.
 * parts 를 보내면 부품과 태그 전체를 바꾸며, 태그는 images[].tags 로 보낸다 (imageKey 는 이 게시물의 사진).
 */
data class PostUpdateRequest(
    @Schema(description = "빌드 방향", example = "CIRCUIT")
    val buildStyle: BuildStyle?,

    @Schema(description = "본문 (최대 1000자, 빈 문자열이면 삭제)")
    @field:Size(max = 1000)
    val content: String?,

    @Schema(description = "부품 전체 (유지할 부품은 postPartId 포함)")
    @field:Size(max = 30)
    val parts: List<@NotNull @Valid PostPartRequest>?,

    @Schema(description = "사진별 태그. parts 와 함께만 보낼 수 있다 (없이 보내면 VALIDATION_FAILED)")
    @field:Size(max = 10)
    val images: List<@NotNull @Valid PostImageRequest>?,
)
