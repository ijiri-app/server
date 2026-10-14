package ijiri.ijiriserver.domain.wishlist.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "어느 게시물의 어느 부품"(postPartId)을 담은 기록. 담기 수 집계를 위해 게시물 id 와 작성자 id 를 복사해 둔다.
 * 작성자 본인의 담기는 담기 수에 세지 않고, 담은 회원이 탈퇴하면 hidden 이 되어 세지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "wishlist_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wishlist_item_member_id_post_part_id",
                columnNames = {"member_id", "post_part_id"}
        ),
        indexes = {
                @Index(name = "idx_wishlist_item_post_part_id", columnList = "post_part_id"),
                @Index(name = "idx_wishlist_item_post_id", columnList = "post_id"),
                @Index(name = "idx_wishlist_item_post_author_id", columnList = "post_author_id")
        }
)
public class WishlistItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "post_part_id", nullable = false, updatable = false)
    private Long postPartId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "post_author_id", nullable = false, updatable = false)
    private Long postAuthorId;

    @Column(name = "hidden", nullable = false)
    private boolean hidden;
}
