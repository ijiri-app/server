package ijiri.ijiriserver.domain.wishlist.adapter.out.persistence;

import ijiri.ijiriserver.domain.wishlist.application.port.out.WishlistItemPort;
import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WishlistItemPersistenceAdapter implements WishlistItemPort {

    private final WishlistItemJpaRepository wishlistItemJpaRepository;

    @Override
    public Optional<WishlistItem> findByMemberIdAndPostPartId(Long memberId, Long postPartId) {
        return wishlistItemJpaRepository.findByMemberIdAndPostPartId(memberId, postPartId);
    }

    @Override
    public Optional<WishlistItem> findByIdAndMemberId(Long id, Long memberId) {
        return wishlistItemJpaRepository.findByIdAndMemberId(id, memberId);
    }

    @Override
    public List<WishlistItem> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long cursor, Limit limit) {
        return wishlistItemJpaRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, cursor, limit);
    }

    @Override
    public List<Object[]> countByPostPartIds(Collection<Long> postPartIds) {
        return wishlistItemJpaRepository.countByPostPartIds(postPartIds);
    }

    @Override
    public long countReceived(Long memberId) {
        return wishlistItemJpaRepository.countReceived(memberId);
    }

    @Override
    public List<Long> findPostPartIds(Long memberId, Collection<Long> postPartIds) {
        return wishlistItemJpaRepository.findPostPartIds(memberId, postPartIds);
    }

    @Override
    public void hideAllByMemberId(Long memberId) {
        wishlistItemJpaRepository.hideAllByMemberId(memberId);
    }

    @Override
    public void deleteAllByMemberIdInBulk(Long memberId) {
        wishlistItemJpaRepository.deleteAllByMemberIdInBulk(memberId);
    }

    @Override
    public void deleteAllByPostId(Long postId) {
        wishlistItemJpaRepository.deleteAllByPostId(postId);
    }

    @Override
    public void deleteAllByPostPartIds(Collection<Long> postPartIds) {
        wishlistItemJpaRepository.deleteAllByPostPartIds(postPartIds);
    }

    @Override
    public void delete(WishlistItem entity) {
        wishlistItemJpaRepository.delete(entity);
    }

    @Override
    public WishlistItem save(WishlistItem entity) {
        return wishlistItemJpaRepository.save(entity);
    }
}
