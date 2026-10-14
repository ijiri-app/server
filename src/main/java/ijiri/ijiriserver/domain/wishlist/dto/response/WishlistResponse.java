package ijiri.ijiriserver.domain.wishlist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.post.dto.PostPartSummary;
import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * wishlist 도메인의 모든 API 응답. 목록은 items + nextCursor, 담기는 id 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WishlistResponse(
        Long id,
        List<Item> items,
        String nextCursor
) {

    public static WishlistResponse added(Long id) {
        return new WishlistResponse(id, null, null);
    }

    public static WishlistResponse list(List<Item> items, Long nextCursor) {
        return new WishlistResponse(null, items, nextCursor != null ? String.valueOf(nextCursor) : null);
    }

    public record Item(
            Long id,
            Long postId,
            Long postPartId,
            String thumbnailUrl,
            PartCategory category,
            String brandName,
            String partName,
            String carModelName,
            LocalDateTime createdAt
    ) {

        public static Item of(WishlistItem item, PostPartSummary part) {
            return new Item(
                    item.getId(),
                    part.postId(),
                    part.postPartId(),
                    part.thumbnailUrl(),
                    part.category(),
                    part.brandName(),
                    part.partName(),
                    part.carModelName(),
                    item.getCreatedAt()
            );
        }
    }
}
