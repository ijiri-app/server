package ijiri.ijiriserver.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostImageRequest(
        @Schema(description = "POST /uploads/images 로 받아 올린 imageKey", example = "posts/tmp/ab12.jpg")
        @NotBlank @Size(max = 100) String imageKey,

        @Schema(description = "올린 사진의 가로 픽셀 (수정할 때는 무시)", example = "1536")
        @Positive Integer width,

        @Schema(description = "올린 사진의 세로 픽셀 (수정할 때는 무시)", example = "2048")
        @Positive Integer height,

        @Schema(description = "이 사진의 부품 태그 (최대 30개)")
        @Size(max = 30) List<@NotNull @Valid PostTagRequest> tags
) {

    public PostImageRequest {
        tags = tags == null ? List.of() : tags;
    }
}
