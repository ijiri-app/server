package ijiri.ijiriserver.domain.block.repository;

import ijiri.ijiriserver.domain.block.entity.MemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {

    Optional<MemberBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<MemberBlock> findAllByBlockerIdOrderByIdDesc(Long blockerId);

    @Query("""
            SELECT CASE WHEN b.blockerId = :memberId THEN b.blockedId ELSE b.blockerId END
            FROM MemberBlock b
            WHERE b.blockerId = :memberId OR b.blockedId = :memberId
            """)
    List<Long> findRelatedMemberIds(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM MemberBlock b WHERE b.blockerId = :memberId OR b.blockedId = :memberId")
    void deleteAllRelatedTo(@Param("memberId") Long memberId);
}
