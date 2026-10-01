package ijiri.ijiriserver.domain.wishlist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * wishlist 도메인의 모든 API 응답. 목록은 items(+nextCursor), 담기는 item, 빼기는 message 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WishlistResponse(
        List<Item> items,
        Long nextCursor,
        Item item,
        String message
) {

    public static WishlistResponse list(List<Item> items, Long nextCursor) {
        return new WishlistResponse(items, nextCursor, null, null);
    }

    public static WishlistResponse single(Item item) {
        return new WishlistResponse(null, null, item, null);
    }

    public static WishlistResponse message(String message) {
        return new WishlistResponse(null, null, null, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            Long id,
            Long partId,
            String partName,
            PartCategory category,
            String brandName,
            LocalDateTime createdAt
    ) {

        public static Item of(WishlistItem item, PartInfo part) {
            return new Item(
                    item.getId(),
                    part.id(),
                    part.name(),
                    part.category(),
                    part.brandName(),
                    item.getCreatedAt()
            );
        }
    }
}
