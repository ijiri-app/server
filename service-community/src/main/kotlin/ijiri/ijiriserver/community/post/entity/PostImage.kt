package ijiri.ijiriserver.community.post.entity

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
import jakarta.persistence.UniqueConstraint

/**
 * 게시물 사진. displayOrder 순서로 보여주고 0 이 피드 썸네일. 크기는 앱이 줄인 뒤 보낸 값이다.
 * 사진은 게시물을 만든 뒤 바꿀 수 없다.
 */
@Entity
@Table(
    name = "post_image",
    uniqueConstraints = [UniqueConstraint(name = "uk_post_image_image_key", columnNames = ["image_key"])],
    indexes = [Index(name = "idx_post_image_post_id", columnList = "post_id")],
)
class PostImage(
    post: Post,
    imageKey: String,
    url: String,
    width: Int,
    height: Int,
    displayOrder: Int,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    var post: Post = post
        protected set

    @Column(name = "image_key", nullable = false, length = 100, updatable = false)
    var imageKey: String = imageKey
        protected set

    @Column(name = "url", nullable = false, length = 500, updatable = false)
    var url: String = url
        protected set

    @Column(name = "width", nullable = false, updatable = false)
    var width: Int = width
        protected set

    @Column(name = "height", nullable = false, updatable = false)
    var height: Int = height
        protected set

    @Column(name = "display_order", nullable = false, updatable = false)
    var displayOrder: Int = displayOrder
        protected set
}
