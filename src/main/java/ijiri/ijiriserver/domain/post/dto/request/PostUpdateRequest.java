package ijiri.ijiriserver.domain.post.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로). 사진은 바꿀 수 없다.
 * parts 를 보내면 부품과 태그 전체를 바꾸며, 태그는 images[].tags 로 보낸다 (imageKey 는 이 게시물의 사진).
 */
public record PostUpdateRequest(
        @Schema(description = "빌드 방향", example = "CIRCUIT")
        BuildStyle buildStyle,

        @Schema(description = "본문 (최대 1000자, 빈 문자열이면 삭제)")
        @Size(max = 1000) String content,

        @Schema(description = "부품 전체 (유지할 부품은 postPartId 포함)")
        @Size(max = 30) List<@NotNull @Valid PostPartRequest> parts,

        @Schema(description = "사진별 태그. parts 와 함께만 보낼 수 있다 (없이 보내면 VALIDATION_FAILED)")
        @Size(max = 10) List<@NotNull @Valid PostImageRequest> images
) {
}
