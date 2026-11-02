package ijiri.ijiriserver.community.wishlist.application.port.`in`

import ijiri.ijiriserver.community.wishlist.application.data.response.WishlistResponse

interface GetWishlistUseCase {

    fun getWishlist(memberId: Long, cursor: Long?, size: Int): WishlistResponse
}
