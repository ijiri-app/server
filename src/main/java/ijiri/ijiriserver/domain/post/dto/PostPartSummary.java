package ijiri.ijiriserver.domain.post.dto;

import ijiri.ijiriserver.domain.part.entity.PartCategory;

/**
 * 위시리스트 목록에 보여줄 게시물 부품 정보.
 */
public record PostPartSummary(
        Long postPartId,
        Long postId,
        String thumbnailUrl,
        PartCategory category,
        String brandName,
        String partName,
        String carModelName
) {
}
