package ijiri.ijiriserver.domain.ownedcar.application.data.request

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

class OwnedCarCreateRequest(
    @Schema(description = "트림 ID. 모델·세대는 트림으로 정해진다", example = "21")
    @field:NotNull @field:Positive val trimId: Long?,

    @Schema(description = "연식. 세대의 판매 기간 안에서만", example = "2023")
    @field:NotNull val year: Int?,

    @Schema(description = "빌드 방향 (선택)", example = "DAILY")
    val buildStyle: BuildStyle?,

    nickname: String?,
) {

    // 공백뿐이면 null, 아니면 앞뒤 공백을 지운 값으로 검증·저장한다
    @Schema(description = "차량 별칭 (선택, 20자 이하)", example = "흰둥이")
    @field:Size(max = 20)
    val nickname: String? = nickname?.trim()?.ifEmpty { null }
}
