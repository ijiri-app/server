package ijiri.ijiriserver.domain.post.entity

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot
import ijiri.ijiriserver.global.entity.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table

/**
 * 게시물. 보유 차량을 가리키고, 피드 필터·카드용으로 차종·트림·연식을 복사해 둔다 (보유 차량의 트림은 바뀌지 않는다).
 * partCount 는 피드 카드에 보여줄 부품 수로, 부품을 바꿀 때마다 다시 센다.
 */
@Entity
@Table(
    name = "post",
    indexes = [
        Index(name = "idx_post_member_id", columnList = "member_id"),
        Index(name = "idx_post_owned_car_id", columnList = "owned_car_id"),
        Index(name = "idx_post_status_car_model_id", columnList = "status, car_model_id"),
    ],
)
class Post(
    memberId: Long,
    ownedCarId: Long,
    carModelId: Long,
    carGenerationId: Long,
    carTrimId: Long,
    modelYear: Int,
    buildStyle: BuildStyle,
    content: String?,
    status: PostStatus,
    partCount: Int = 0,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "owned_car_id", nullable = false, updatable = false)
    var ownedCarId: Long = ownedCarId
        protected set

    @Column(name = "car_model_id", nullable = false, updatable = false)
    var carModelId: Long = carModelId
        protected set

    @Column(name = "car_generation_id", nullable = false, updatable = false)
    var carGenerationId: Long = carGenerationId
        protected set

    @Column(name = "car_trim_id", nullable = false, updatable = false)
    var carTrimId: Long = carTrimId
        protected set

    @Column(name = "model_year", nullable = false, updatable = false)
    var modelYear: Int = modelYear
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "build_style", nullable = false, length = 20)
    var buildStyle: BuildStyle = buildStyle
        protected set

    @Column(name = "content", length = 1000)
    var content: String? = content
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PostStatus = status
        protected set

    @Column(name = "part_count", nullable = false)
    var partCount: Int = partCount
        protected set

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var parts: MutableList<PostPart> = mutableListOf()
        protected set

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<PostImage> = mutableListOf()
        protected set

    val thumbnail: PostImage?
        get() = images.firstOrNull()

    fun changeBuildStyle(buildStyle: BuildStyle) {
        this.buildStyle = buildStyle
    }

    fun changeContent(content: String?) {
        this.content = content
    }

    fun addImage(image: PostImage) {
        images.add(image)
    }

    // 유지되는 부품은 같은 PostPart 를 다시 넣어 postPartId(위시리스트가 가리키는 id)를 지킨다
    fun replaceParts(newParts: List<PostPart>) {
        parts.clear()
        parts.addAll(newParts)
        partCount = newParts.size
    }

    fun hide() {
        if (status == PostStatus.PUBLIC) {
            status = PostStatus.HIDDEN
        }
    }

    fun isPublic(): Boolean = status == PostStatus.PUBLIC

    fun isWrittenBy(memberId: Long?): Boolean = this.memberId == memberId

    companion object {
        fun of(memberId: Long, car: OwnedCarSnapshot, buildStyle: BuildStyle, content: String?): Post =
            Post(
                memberId = memberId,
                ownedCarId = car.ownedCarId,
                carModelId = car.spec.carModelId,
                carGenerationId = car.spec.generationId,
                carTrimId = car.spec.trimId,
                modelYear = car.modelYear,
                buildStyle = buildStyle,
                content = content,
                status = PostStatus.PUBLIC,
            )
    }
}
