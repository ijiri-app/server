package ijiri.ijiriserver.domain.post.dto.request;

import ijiri.ijiriserver.domain.part.entity.PartCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 검색한 기존 부품이면 partId 만, 검색 결과에 없으면 partName + category(+ brandName) 로 새로 입력한다.
 * partId 가 있으면 나머지 부품 정보는 무시한다.
 */
public record PartTagRequest(
        @Schema(description = "사진 너비에 대한 가로 위치 비율 (0~1)", example = "0.42")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double x,

        @Schema(description = "사진 높이에 대한 세로 위치 비율 (0~1)", example = "0.63")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double y,

        @Schema(description = "기존 부품 ID", example = "12")
        @Positive Long partId,

        @Schema(description = "새 부품 이름", example = "N 퍼포먼스 머플러")
        @Size(max = 100) String partName,

        @Schema(description = "새 부품 브랜드 (선택, 없으면 새로 등록)", example = "현대 N")
        @Size(max = 50) String brandName,

        @Schema(description = "새 부품 분류", example = "POWERTRAIN")
        PartCategory category
) {
}
