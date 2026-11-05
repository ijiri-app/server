package ijiri.ijiriserver.community.wishlist.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.common.contract.PartCategory
import ijiri.ijiriserver.community.post.application.data.PostPartSummary
import ijiri.ijiriserver.community.wishlist.entity.WishlistItem
import java.time.LocalDateTime

/**
 * wishlist 도메인의 모든 API 응답. 목록은 items + nextCursor, 담기는 id 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WishlistResponse(
    val id: Long? = null,
    val items: List<Item>? = null,
    val nextCursor: String? = null,
) {

    data class Item(
        val id: Long,
        val postId: Long,
        val postPartId: Long,
        val thumbnailUrl: String?,
        val category: PartCategory,
        val brandName: String?,
        val partName: String?,
        val carModelName: String?,
        val createdAt: LocalDateTime,
    ) {

        companion object {
            fun of(item: WishlistItem, part: PostPartSummary): Item =
                Item(
                    item.id,
                    part.postId,
                    part.postPartId,
                    part.thumbnailUrl,
                    part.category,
                    part.brandName,
                    part.partName,
                    part.carModelName,
                    item.createdAt,
                )
        }
    }

    companion object {
        fun added(id: Long): WishlistResponse = WishlistResponse(id = id)

        fun list(items: List<Item>, nextCursor: Long?): WishlistResponse =
            WishlistResponse(items = items, nextCursor = nextCursor?.toString())
    }
}
