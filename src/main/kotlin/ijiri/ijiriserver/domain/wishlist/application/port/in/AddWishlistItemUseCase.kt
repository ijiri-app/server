package ijiri.ijiriserver.domain.wishlist.application.port.`in`

import ijiri.ijiriserver.domain.wishlist.application.data.WishlistAddResult

interface AddWishlistItemUseCase {

    fun add(memberId: Long, postPartId: Long): WishlistAddResult
}
