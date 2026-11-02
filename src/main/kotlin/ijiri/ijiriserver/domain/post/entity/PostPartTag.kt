package ijiri.ijiriserver.domain.post.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 사진 위 부품 위치. x, y 는 사진 크기에 대한 비율(0~1, 왼쪽 위가 0)이라 화면 크기가 달라도 같은 자리에 찍힌다.
 */
@Entity
@Table(
    name = "post_part_tag",
    indexes = [
        Index(name = "idx_post_part_tag_post_part_id", columnList = "post_part_id"),
        Index(name = "idx_post_part_tag_post_image_id", columnList = "post_image_id"),
    ],
)
class PostPartTag(
    postPart: PostPart,
    postImage: PostImage,
    x: Double,
    y: Double,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_part_id", nullable = false, updatable = false)
    var postPart: PostPart = postPart
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_image_id", nullable = false, updatable = false)
    var postImage: PostImage = postImage
        protected set

    @Column(name = "x", nullable = false)
    var x: Double = x
        protected set

    @Column(name = "y", nullable = false)
    var y: Double = y
        protected set
}
