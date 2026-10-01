package ijiri.ijiriserver.domain.wishlist.repository;

import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByMemberIdAndPartId(Long memberId, Long partId);

    Optional<WishlistItem> findByIdAndMemberId(Long id, Long memberId);

    List<WishlistItem> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long cursor, Limit limit);

    @Query("SELECT w.partId FROM WishlistItem w WHERE w.memberId = :memberId AND w.partId IN :partIds")
    List<Long> findPartIds(@Param("memberId") Long memberId, @Param("partIds") Collection<Long> partIds);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.memberId = :memberId")
    void deleteAllByMemberIdInBulk(@Param("memberId") Long memberId);
}
