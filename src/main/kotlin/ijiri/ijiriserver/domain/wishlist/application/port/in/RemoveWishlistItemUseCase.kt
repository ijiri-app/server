package ijiri.ijiriserver.domain.wishlist.application.port.`in`

interface RemoveWishlistItemUseCase {

    fun remove(memberId: Long, wishlistItemId: Long)
}
