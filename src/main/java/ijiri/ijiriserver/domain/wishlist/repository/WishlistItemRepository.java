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

    Optional<WishlistItem> findByMemberIdAndPostPartId(Long memberId, Long postPartId);

    Optional<WishlistItem> findByIdAndMemberId(Long id, Long memberId);

    List<WishlistItem> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long cursor, Limit limit);

    // 작성자 본인과 탈퇴한 회원의 담기는 세지 않는다
    @Query("""
            SELECT w.postPartId, COUNT(w) FROM WishlistItem w
            WHERE w.postPartId IN :postPartIds AND w.hidden = false AND w.memberId <> w.postAuthorId
            GROUP BY w.postPartId
            """)
    List<Object[]> countByPostPartIds(@Param("postPartIds") Collection<Long> postPartIds);

    @Query("""
            SELECT COUNT(w) FROM WishlistItem w
            WHERE w.postAuthorId = :memberId AND w.hidden = false AND w.memberId <> w.postAuthorId
            """)
    long countReceived(@Param("memberId") Long memberId);

    @Query("SELECT w.postPartId FROM WishlistItem w WHERE w.memberId = :memberId AND w.postPartId IN :postPartIds")
    List<Long> findPostPartIds(@Param("memberId") Long memberId, @Param("postPartIds") Collection<Long> postPartIds);

    @Modifying
    @Query("UPDATE WishlistItem w SET w.hidden = true WHERE w.memberId = :memberId")
    void hideAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.memberId = :memberId")
    void deleteAllByMemberIdInBulk(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.postId = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.postPartId IN :postPartIds")
    void deleteAllByPostPartIds(@Param("postPartIds") Collection<Long> postPartIds);
}
