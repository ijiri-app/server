package ijiri.ijiriserver.domain.wishlist.application.data

/**
 * 담기 결과. created 가 false 면 이미 담겨 있던 항목이다 (응답 200, 같은 id).
 */
data class WishlistAddResult(
    val id: Long,
    val created: Boolean,
)
