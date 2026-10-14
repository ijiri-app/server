package ijiri.ijiriserver.domain.post.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @Schema(description = "내 보유 차량 ID", example = "5")
        @NotNull @Positive Long ownedCarId,

        @Schema(description = "빌드 방향", example = "DAILY")
        @NotNull BuildStyle buildStyle,

        @Schema(description = "본문 (선택, 최대 1000자)")
        @Size(max = 1000) String content,

        @Schema(description = "부품 0~30개 (순정 차량은 비워도 된다)")
        @Size(max = 30) List<@NotNull @Valid PostPartRequest> parts,

        @Schema(description = "사진 1~10장. 순서대로 보여주고 첫 장이 피드 썸네일. width, height 필수")
        @NotNull @Size(min = 1, max = 10) List<@NotNull @Valid PostImageRequest> images
) {

    public PostCreateRequest {
        parts = parts == null ? List.of() : parts;
    }
}
