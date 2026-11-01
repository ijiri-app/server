package ijiri.ijiriserver.domain.wishlist.application.port.in;

import ijiri.ijiriserver.domain.wishlist.application.data.WishlistAddResult;

public interface AddWishlistItemUseCase {

    WishlistAddResult add(Long memberId, Long postPartId);
}
