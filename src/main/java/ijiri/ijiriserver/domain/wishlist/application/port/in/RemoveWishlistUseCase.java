package ijiri.ijiriserver.domain.wishlist.application.port.in;

public interface RemoveWishlistUseCase {

    void remove(Long memberId, Long wishlistItemId);
}
