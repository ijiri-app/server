package ijiri.ijiriserver.community.wishlist.application.port.`in`

interface RemoveWishlistItemUseCase {

    fun remove(memberId: Long, wishlistItemId: Long)
}
