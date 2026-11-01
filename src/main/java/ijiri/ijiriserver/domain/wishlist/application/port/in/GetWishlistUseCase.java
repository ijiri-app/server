package ijiri.ijiriserver.domain.wishlist.application.port.in;

import ijiri.ijiriserver.domain.wishlist.application.data.response.WishlistResponse;

public interface GetWishlistUseCase {

    WishlistResponse getWishlist(Long memberId, Long cursor, int size);
}
