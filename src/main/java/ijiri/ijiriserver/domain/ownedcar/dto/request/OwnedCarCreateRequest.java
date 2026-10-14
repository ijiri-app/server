package ijiri.ijiriserver.domain.ownedcar.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OwnedCarCreateRequest(
        @Schema(description = "트림 ID. 모델·세대는 트림으로 정해진다", example = "21")
        @NotNull @Positive Long trimId,

        @Schema(description = "연식. 세대의 판매 기간 안에서만", example = "2023")
        @NotNull Integer year,

        @Schema(description = "빌드 방향 (선택)", example = "DAILY")
        BuildStyle buildStyle,

        @Schema(description = "차량 별칭 (선택, 20자 이하)", example = "흰둥이")
        @Size(max = 20) String nickname
) {

    public OwnedCarCreateRequest {
        nickname = nickname == null || nickname.isBlank() ? null : nickname.strip();
    }
}
