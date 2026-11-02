package ijiri.ijiriserver.community.block.entity

import ijiri.ijiriserver.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * blocker 가 blocked 를 차단. 서로의 게시물이 피드·상세·작성자 게시물 목록에서 보이지 않는다.
 */
@Entity
@Table(
    name = "member_block",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_block_blocker_id_blocked_id",
            columnNames = ["blocker_id", "blocked_id"],
        ),
    ],
    indexes = [Index(name = "idx_member_block_blocked_id", columnList = "blocked_id")],
)
class MemberBlock(
    blockerId: Long,
    blockedId: Long,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "blocker_id", nullable = false, updatable = false)
    var blockerId: Long = blockerId
        protected set

    @Column(name = "blocked_id", nullable = false, updatable = false)
    var blockedId: Long = blockedId
        protected set
}
