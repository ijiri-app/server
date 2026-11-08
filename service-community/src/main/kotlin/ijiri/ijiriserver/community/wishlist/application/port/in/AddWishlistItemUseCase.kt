package ijiri.ijiriserver.community.wishlist.application.port.`in`

import ijiri.ijiriserver.community.wishlist.application.data.WishlistAddResult

interface AddWishlistItemUseCase {

    fun add(memberId: Long, postPartId: Long): WishlistAddResult
}
