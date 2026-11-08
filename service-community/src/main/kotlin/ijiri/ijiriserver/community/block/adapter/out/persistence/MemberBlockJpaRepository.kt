package ijiri.ijiriserver.community.block.adapter.out.persistence

import ijiri.ijiriserver.community.block.entity.MemberBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberBlockJpaRepository : JpaRepository<MemberBlock, Long> {

    fun findByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): MemberBlock?

    fun findAllByBlockerIdOrderByIdDesc(blockerId: Long): List<MemberBlock>

    @Query(
        """
        SELECT CASE WHEN b.blockerId = :memberId THEN b.blockedId ELSE b.blockerId END
        FROM MemberBlock b
        WHERE b.blockerId = :memberId OR b.blockedId = :memberId
        """,
    )
    fun findRelatedMemberIds(@Param("memberId") memberId: Long): List<Long>

    @Modifying
    @Query("DELETE FROM MemberBlock b WHERE b.blockerId = :memberId OR b.blockedId = :memberId")
    fun deleteAllRelatedTo(@Param("memberId") memberId: Long)
}
