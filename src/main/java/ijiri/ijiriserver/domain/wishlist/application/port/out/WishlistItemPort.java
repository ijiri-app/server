package ijiri.ijiriserver.domain.wishlist.application.port.out;

import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Limit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WishlistItemPort {

    Optional<WishlistItem> findByMemberIdAndPostPartId(Long memberId, Long postPartId);

    Optional<WishlistItem> findByIdAndMemberId(Long id, Long memberId);

    List<WishlistItem> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long cursor, Limit limit);

    List<Object[]> countByPostPartIds(Collection<Long> postPartIds);

    long countReceived(Long memberId);

    List<Long> findPostPartIds(Long memberId, Collection<Long> postPartIds);

    void hideAllByMemberId(Long memberId);

    void deleteAllByMemberIdInBulk(Long memberId);

    void deleteAllByPostId(Long postId);

    void deleteAllByPostPartIds(Collection<Long> postPartIds);

    void delete(WishlistItem entity);

    WishlistItem save(WishlistItem entity);
}
