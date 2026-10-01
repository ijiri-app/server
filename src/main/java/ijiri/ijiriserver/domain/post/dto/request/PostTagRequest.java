package ijiri.ijiriserver.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostTagRequest(
        @Schema(description = "parts 의 ref", example = "p1")
        @NotBlank String ref,

        @Schema(description = "사진 너비에 대한 가로 위치 비율 (0~1)", example = "0.42")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double x,

        @Schema(description = "사진 높이에 대한 세로 위치 비율 (0~1)", example = "0.71")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double y
) {
}
