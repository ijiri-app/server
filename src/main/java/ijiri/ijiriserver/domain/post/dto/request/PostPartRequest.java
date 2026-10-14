package ijiri.ijiriserver.domain.post.dto.request;

import ijiri.ijiriserver.domain.part.entity.PartCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 게시물의 부품. 기존 부품은 partId, 검색에 없으면 brandName(선택) + partName 으로 새로 입력한다.
 * partId 가 있으면 이름은 무시한다. 수정할 때 이미 달려 있던 부품은 postPartId 를 함께 보내면 담기가 유지되며,
 * 이때 다른 부품으로 바꿀 수는 없다 (바꾸려면 postPartId 없이 새 부품으로 보낸다).
 */
public record PostPartRequest(
        @Schema(description = "이 요청 안에서 태그가 가리키는 이름", example = "p1")
        @NotBlank @Size(max = 20) String ref,

        @Schema(description = "분류", example = "CHASSIS")
        @NotNull PartCategory category,

        @Schema(description = "기존 부품 ID", example = "88")
        @Positive Long partId,

        @Schema(description = "새 부품의 브랜드 (선택)", example = "RAYS")
        @Size(max = 50) String brandName,

        @Schema(description = "새 부품 이름", example = "TE37 SAGA 18\"")
        @Size(max = 100) String partName,

        @Schema(description = "수정할 때만: 유지할 기존 게시물 부품 ID", example = "11")
        @Positive Long postPartId
) {
}
