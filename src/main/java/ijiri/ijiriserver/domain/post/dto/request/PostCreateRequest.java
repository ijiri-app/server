package ijiri.ijiriserver.domain.post.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @Schema(description = "보유 차량 ID", example = "1")
        @NotNull @Positive Long ownedCarId,

        @Schema(description = "빌드 방향", example = "STREET")
        @NotNull BuildDirection buildDirection,

        @Schema(description = "본문 (선택, 최대 2000자)")
        @Size(max = 2000) String content,

        @Schema(description = "사진 1~10장. 순서대로 보여주고 첫 장이 피드 썸네일")
        @NotNull @Size(min = 1, max = 10) List<@NotNull @Valid PostImageRequest> images
) {
}
