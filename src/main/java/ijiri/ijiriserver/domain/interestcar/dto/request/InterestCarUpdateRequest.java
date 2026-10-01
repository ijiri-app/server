package ijiri.ijiriserver.domain.interestcar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InterestCarUpdateRequest(
        @Schema(description = "관심 차종 ID 목록 (1~10개). 순서 = 피드 탭 순서", example = "[3, 11, 15]")
        @NotNull @Size(min = 1, max = 10) List<@NotNull @Positive Long> carModelIds
) {
}
