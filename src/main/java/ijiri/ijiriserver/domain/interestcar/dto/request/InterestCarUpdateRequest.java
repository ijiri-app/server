package ijiri.ijiriserver.domain.interestcar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InterestCarUpdateRequest(
        @Schema(
                description = "관심 차종 ID 목록 (최대 30개). 순서 = 화면에 보이는 순서, 빈 배열이면 전체 삭제",
                example = "[3, 17, 25]"
        )
        @NotNull @Size(max = 30) List<@NotNull @Positive Long> carModelIds
) {
}
