package ijiri.ijiriserver.domain.ownedcar.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 보유 차량 정보를 통째로 바꾼다 (보내지 않은 선택 항목은 비워진다).
 */
public record OwnedCarUpdateRequest(
        @Schema(description = "차종(모델) ID", example = "1")
        @NotNull Long carModelId,

        @Schema(description = "세대 ID", example = "1")
        @NotNull Long carGenerationId,

        @Schema(description = "트림 ID (선택)", example = "1")
        Long carTrimId,

        @Schema(description = "연식 (선택)", example = "2023")
        @Min(1950) @Max(2100) Integer modelYear,

        @Schema(description = "빌드 방향 (선택)", example = "STREET")
        BuildDirection buildDirection
) {
}
