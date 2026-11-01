package ijiri.ijiriserver.domain.wishlist.application.port.in;

import ijiri.ijiriserver.domain.wishlist.application.data.WishlistAddResult;

public interface AddWishlistUseCase {

    WishlistAddResult add(Long memberId, Long postPartId);
}
