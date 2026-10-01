package ijiri.ijiriserver.domain.ownedcar.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로). 트림은 바꿀 수 없다.
 */
public record OwnedCarUpdateRequest(
        @Schema(description = "연식", example = "2023")
        Integer year,

        @Schema(description = "빌드 방향", example = "STANCE")
        BuildStyle buildStyle,

        @Schema(description = "차량 별칭 (20자 이하, 빈 문자열이면 삭제)", example = "흰둥이")
        @Size(max = 20) String nickname,

        @Schema(description = "OWNED(지금 타는 차) / PAST(이전 차량)", example = "PAST")
        OwnedCarStatus status
) {
}
