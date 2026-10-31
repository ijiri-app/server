package ijiri.ijiriserver.domain.wishlist.application.port.in;

import ijiri.ijiriserver.domain.wishlist.application.data.WishlistAddResult;
import ijiri.ijiriserver.domain.wishlist.application.data.response.WishlistResponse;

public interface WishlistUseCase {

    WishlistAddResult add(Long memberId, Long postPartId);

    void remove(Long memberId, Long wishlistItemId);

    WishlistResponse getWishlist(Long memberId, Long cursor, int size);
}
