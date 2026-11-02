package ijiri.ijiriserver.community.wishlist.entity

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
 * "어느 게시물의 어느 부품"(postPartId)을 담은 기록. 담기 수 집계를 위해 게시물 id 와 작성자 id 를 복사해 둔다.
 * 작성자 본인의 담기는 담기 수에 세지 않고, 담은 회원이 탈퇴하면 hidden 이 되어 세지 않는다.
 */
@Entity
@Table(
    name = "wishlist_item",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_wishlist_item_member_id_post_part_id",
            columnNames = ["member_id", "post_part_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_wishlist_item_post_part_id", columnList = "post_part_id"),
        Index(name = "idx_wishlist_item_post_id", columnList = "post_id"),
        Index(name = "idx_wishlist_item_post_author_id", columnList = "post_author_id"),
    ],
)
class WishlistItem(
    memberId: Long,
    postPartId: Long,
    postId: Long,
    postAuthorId: Long,
    hidden: Boolean = false,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "post_part_id", nullable = false, updatable = false)
    var postPartId: Long = postPartId
        protected set

    @Column(name = "post_id", nullable = false, updatable = false)
    var postId: Long = postId
        protected set

    @Column(name = "post_author_id", nullable = false, updatable = false)
    var postAuthorId: Long = postAuthorId
        protected set

    @Column(name = "hidden", nullable = false)
    var hidden: Boolean = hidden
        protected set
}
