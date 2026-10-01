package ijiri.ijiriserver.domain.wishlist.service;

import ijiri.ijiriserver.domain.wishlist.dto.response.WishlistResponse;

import java.util.Collection;
import java.util.Set;

public interface WishlistService {

    WishlistResponse add(Long memberId, Long partId);

    WishlistResponse remove(Long memberId, Long wishlistItemId);

    WishlistResponse getWishlist(Long memberId, Long cursor, int size);

    Set<Long> getWishlistedPartIds(Long memberId, Collection<Long> partIds);
}
