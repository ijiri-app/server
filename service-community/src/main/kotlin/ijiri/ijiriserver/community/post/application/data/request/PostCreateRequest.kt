package ijiri.ijiriserver.community.post.application.data.request

import ijiri.ijiriserver.common.contract.BuildStyle
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class PostCreateRequest(
    @Schema(description = "내 보유 차량 ID", example = "5")
    @field:NotNull @field:Positive
    val ownedCarId: Long?,

    @Schema(description = "빌드 방향", example = "DAILY")
    @field:NotNull
    val buildStyle: BuildStyle?,

    @Schema(description = "본문 (선택, 최대 1000자)")
    @field:Size(max = 1000)
    val content: String?,

    // 보내지 않거나 null 이면 부품 없음 (읽을 때 orEmpty())
    @Schema(description = "부품 0~30개 (순정 차량은 비워도 된다)")
    @field:Size(max = 30)
    val parts: List<@NotNull @Valid PostPartRequest>?,

    @Schema(description = "사진 1~10장. 순서대로 보여주고 첫 장이 피드 썸네일. width, height 필수")
    @field:NotNull @field:Size(min = 1, max = 10)
    val images: List<@NotNull @Valid PostImageRequest>?,
)
