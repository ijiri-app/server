package ijiri.ijiriserver.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostImageRequest(
        @Schema(description = "POST /uploads/images 로 받은 imageId", example = "101")
        @NotNull @Positive Long imageId,

        @Schema(description = "이 사진의 부품 태그 (최대 20개)")
        @Size(max = 20) List<@NotNull @Valid PartTagRequest> tags
) {

    public PostImageRequest {
        tags = tags == null ? List.of() : tags;
    }
}
