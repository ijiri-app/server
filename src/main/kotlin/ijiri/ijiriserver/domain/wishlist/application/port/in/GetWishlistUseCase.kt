package ijiri.ijiriserver.domain.wishlist.application.port.`in`

import ijiri.ijiriserver.domain.wishlist.application.data.response.WishlistResponse

interface GetWishlistUseCase {

    fun getWishlist(memberId: Long, cursor: Long?, size: Int): WishlistResponse
}
