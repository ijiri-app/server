package ijiri.ijiriserver.domain.post.entity

import ijiri.ijiriserver.domain.part.entity.PartCategory
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * 게시물에 달린 부품(postPartId). 위시리스트는 이 id 를 담는다. 분류는 작성자가 고른 값.
 * 태그는 0개 이상이고 여러 사진에 걸쳐 있을 수 있다.
 */
@Entity
@Table(
    name = "post_part",
    indexes = [
        Index(name = "idx_post_part_post_id", columnList = "post_id"),
        Index(name = "idx_post_part_part_id", columnList = "part_id"),
    ],
)
class PostPart(
    post: Post,
    partId: Long,
    category: PartCategory,
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

    @Column(name = "part_id", nullable = false, updatable = false)
    var partId: Long = partId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    var category: PartCategory = category
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder
        protected set

    @OneToMany(mappedBy = "postPart", cascade = [CascadeType.ALL], orphanRemoval = true)
    var tags: MutableList<PostPartTag> = mutableListOf()
        protected set

    fun reorder(category: PartCategory, displayOrder: Int) {
        this.category = category
        this.displayOrder = displayOrder
    }

    fun clearTags() {
        tags.clear()
    }

    fun addTag(image: PostImage, x: Double, y: Double) {
        tags.add(
            PostPartTag(
                postPart = this,
                postImage = image,
                x = x,
                y = y,
            )
        )
    }

    companion object {
        fun of(post: Post, partId: Long, category: PartCategory, displayOrder: Int): PostPart =
            PostPart(
                post = post,
                partId = partId,
                category = category,
                displayOrder = displayOrder,
            )
    }
}
