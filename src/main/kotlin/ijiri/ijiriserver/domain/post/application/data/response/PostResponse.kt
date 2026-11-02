package ijiri.ijiriserver.domain.post.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle
import ijiri.ijiriserver.domain.carmodel.entity.CarSpec
import ijiri.ijiriserver.domain.member.entity.Member
import ijiri.ijiriserver.domain.part.application.data.PartInfo
import ijiri.ijiriserver.domain.part.entity.PartCategory
import ijiri.ijiriserver.domain.post.entity.Post
import ijiri.ijiriserver.domain.post.entity.PostImage
import ijiri.ijiriserver.domain.post.entity.PostPart
import java.time.LocalDateTime

/**
 * post 도메인의 모든 API 응답. 상세는 게시물 필드, 목록(피드, 회원 게시물)은 items + nextCursor,
 * 작성은 id 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostResponse(
    val id: Long? = null,
    val author: Author? = null,
    val car: Car? = null,
    val content: String? = null,
    val images: List<Image>? = null,
    val parts: List<Part>? = null,
    val wishCount: Long? = null,
    // is 로 시작하는 Kotlin 프로퍼티라 JSON 이름을 고정한다
    @JsonProperty("isMine")
    val isMine: Boolean? = null,
    val createdAt: LocalDateTime? = null,
    val items: List<Card>? = null,
    val nextCursor: String? = null,
) {

    data class Author(
        val id: Long,
        val nickname: String,
        val profileImageUrl: String?,
        val statusMessage: String?,
    ) {

        companion object {
            fun from(member: Member): Author =
                Author(
                    member.id,
                    member.nickname,
                    member.profileImageUrl,
                    member.statusMessage,
                )
        }
    }

    data class Car(
        val ownedCarId: Long,
        val carModelId: Long,
        val carModelName: String,
        val generationCode: String,
        val trimName: String,
        val year: Int,
        val buildStyle: BuildStyle,
    ) {

        companion object {
            fun of(post: Post, spec: CarSpec): Car =
                Car(
                    post.ownedCarId,
                    spec.carModelId,
                    spec.modelName,
                    spec.generationCode,
                    spec.trimName,
                    post.modelYear,
                    post.buildStyle,
                )
        }
    }

    // imageKey 는 게시물 수정에서 태그를 이 사진에 다시 걸 때 쓴다
    data class Image(
        val id: Long,
        val imageKey: String,
        val url: String,
        val width: Int,
        val height: Int,
        val tags: List<Tag>,
    ) {

        companion object {
            fun of(image: PostImage, parts: List<PostPart>): Image =
                Image(
                    image.id,
                    image.imageKey,
                    image.url,
                    image.width,
                    image.height,
                    parts
                        .flatMap { it.tags }
                        .filter { it.postImage.id == image.id }
                        .map { Tag(it.postPart.id, it.x, it.y) },
                )
        }
    }

    data class Tag(
        val postPartId: Long,
        val x: Double,
        val y: Double,
    )

    data class Part(
        val postPartId: Long,
        val partId: Long,
        val category: PartCategory,
        val brandName: String?,
        val partName: String?,
        val wishCount: Long,
        val wished: Boolean,
    ) {

        companion object {
            fun of(part: PostPart, info: PartInfo?, wishCount: Long, wished: Boolean): Part =
                Part(
                    part.id,
                    part.partId,
                    part.category,
                    info?.brandName,
                    info?.name,
                    wishCount,
                    wished,
                )
        }
    }

    /**
     * 피드 2열 벽돌형 카드. 썸네일 크기로 카드 높이를 정한다. hidden 은 내 게시물 목록에서 숨김 처리된 게시물만 true.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Card(
        val postId: Long,
        val thumbnailUrl: String?,
        val thumbWidth: Int?,
        val thumbHeight: Int?,
        val carModelName: String?,
        val year: Int,
        val partCount: Int,
        val hidden: Boolean?,
    ) {

        companion object {
            fun of(post: Post, carModelName: String?): Card {
                val thumbnail = post.thumbnail
                return Card(
                    post.id,
                    thumbnail?.url,
                    thumbnail?.width,
                    thumbnail?.height,
                    carModelName,
                    post.modelYear,
                    post.partCount,
                    if (post.isPublic()) null else true,
                )
            }
        }
    }

    companion object {
        fun created(id: Long): PostResponse = PostResponse(id = id)

        fun cards(items: List<Card>, nextCursor: Long?): PostResponse =
            PostResponse(
                items = items,
                nextCursor = nextCursor?.toString(),
            )

        fun detail(
            post: Post,
            author: Member?,
            spec: CarSpec,
            partInfos: Map<Long, PartInfo>,
            wishCounts: Map<Long, Long>,
            wished: Set<Long>,
            isMine: Boolean,
        ): PostResponse {
            val parts = post.parts
                .sortedWith(compareBy<PostPart> { it.category }.thenBy { it.displayOrder })
                .map {
                    Part.of(
                        it,
                        partInfos[it.partId],
                        wishCounts.getOrDefault(it.id, 0L),
                        wished.contains(it.id),
                    )
                }
            return PostResponse(
                id = post.id,
                author = author?.let { Author.from(it) },
                car = Car.of(post, spec),
                content = post.content,
                images = post.images.map { Image.of(it, post.parts) },
                parts = parts,
                wishCount = parts.sumOf { it.wishCount },
                isMine = isMine,
                createdAt = post.createdAt,
            )
        }
    }
}
