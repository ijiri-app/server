package ijiri.ijiriserver.domain.post.dto.request;

import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로). images 를 보내면 사진과 태그 전체를 그 목록으로 바꾸고,
 * 빠진 사진은 파일까지 삭제한다.
 */
public record PostUpdateRequest(
        @Schema(description = "보유 차량 ID", example = "1")
        @Positive Long ownedCarId,

        @Schema(description = "빌드 방향", example = "TRACK")
        BuildDirection buildDirection,

        @Schema(description = "본문 (빈 문자열이면 본문 삭제)")
        @Size(max = 2000) String content,

        @Schema(description = "사진 1~10장 전체 (기존 사진은 같은 imageId 로 다시 보낸다)")
        @Size(min = 1, max = 10) List<@NotNull @Valid PostImageRequest> images
) {
}
