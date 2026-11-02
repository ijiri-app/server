package ijiri.ijiriserver.user.ownedcar.application.data.request

import ijiri.ijiriserver.common.contract.BuildStyle
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCarStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로). 트림은 바꿀 수 없다.
 */
data class OwnedCarUpdateRequest(
    @Schema(description = "연식", example = "2023")
    val year: Int?,

    @Schema(description = "빌드 방향", example = "STANCE")
    val buildStyle: BuildStyle?,

    @Schema(description = "차량 별칭 (20자 이하, 빈 문자열이면 삭제)", example = "흰둥이")
    @field:Size(max = 20) val nickname: String?,

    @Schema(description = "OWNED(지금 타는 차) / PAST(이전 차량)", example = "PAST")
    val status: OwnedCarStatus?,
)
