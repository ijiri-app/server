package ijiri.ijiriserver.domain.wishlist.service;

import ijiri.ijiriserver.domain.wishlist.dto.WishlistAddResult;
import ijiri.ijiriserver.domain.wishlist.dto.response.WishlistResponse;

public interface WishlistService {

    WishlistAddResult add(Long memberId, Long postPartId);

    void remove(Long memberId, Long wishlistItemId);

    WishlistResponse getWishlist(Long memberId, Long cursor, int size);
}
