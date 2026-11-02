package ijiri.ijiriserver.user.interestcar.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class InterestCarUpdateRequest(
    @Schema(
        description = "관심 차종 ID 목록 (최대 30개). 순서 = 피드 탭 순서, 빈 배열이면 전체 삭제",
        example = "[3, 11, 15]",
    )
    @field:NotNull
    @field:Size(max = 30)
    val carModelIds: List<@NotNull @Positive Long>?,
)
