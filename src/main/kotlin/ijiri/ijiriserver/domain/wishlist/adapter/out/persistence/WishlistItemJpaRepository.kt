package ijiri.ijiriserver.domain.wishlist.adapter.out.persistence

import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WishlistItemJpaRepository : JpaRepository<WishlistItem, Long> {

    fun findByMemberIdAndPostPartId(memberId: Long, postPartId: Long): WishlistItem?

    fun findByIdAndMemberId(id: Long, memberId: Long): WishlistItem?

    fun findByMemberIdAndIdLessThanOrderByIdDesc(memberId: Long, cursor: Long, limit: Limit): List<WishlistItem>

    // 작성자 본인과 탈퇴한 회원의 담기는 세지 않는다
    @Query(
        """
        SELECT w.postPartId, COUNT(w) FROM WishlistItem w
        WHERE w.postPartId IN :postPartIds AND w.hidden = false AND w.memberId <> w.postAuthorId
        GROUP BY w.postPartId
        """
    )
    fun countByPostPartIds(@Param("postPartIds") postPartIds: Collection<Long>): List<Array<Any>>

    @Query(
        """
        SELECT COUNT(w) FROM WishlistItem w
        WHERE w.postAuthorId = :memberId AND w.hidden = false AND w.memberId <> w.postAuthorId
        """
    )
    fun countReceived(@Param("memberId") memberId: Long): Long

    @Query("SELECT w.postPartId FROM WishlistItem w WHERE w.memberId = :memberId AND w.postPartId IN :postPartIds")
    fun findPostPartIds(
        @Param("memberId") memberId: Long,
        @Param("postPartIds") postPartIds: Collection<Long>,
    ): List<Long>

    @Modifying
    @Query("UPDATE WishlistItem w SET w.hidden = true WHERE w.memberId = :memberId")
    fun hideAllByMemberId(@Param("memberId") memberId: Long)

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.memberId = :memberId")
    fun deleteAllByMemberIdInBulk(@Param("memberId") memberId: Long)

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.postId = :postId")
    fun deleteAllByPostId(@Param("postId") postId: Long)

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.postPartId IN :postPartIds")
    fun deleteAllByPostPartIds(@Param("postPartIds") postPartIds: Collection<Long>)
}
