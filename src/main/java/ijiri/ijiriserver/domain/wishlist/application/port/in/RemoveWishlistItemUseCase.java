package ijiri.ijiriserver.domain.wishlist.application.port.in;

public interface RemoveWishlistItemUseCase {

    void remove(Long memberId, Long wishlistItemId);
}
