package ijiri.ijiriserver.community.post.application.service

import ijiri.ijiriserver.common.contract.BuildStyle
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.community.block.application.port.`in`.GetHiddenMemberIdsUseCase
import ijiri.ijiriserver.community.post.application.data.PostPartSummary
import ijiri.ijiriserver.community.post.application.data.WishTarget
import ijiri.ijiriserver.community.post.application.data.response.PostResponse
import ijiri.ijiriserver.community.post.application.port.`in`.CountMemberPostsUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetFeedUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetMemberPostsUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetPostAuthorIdUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetPostPartSummariesUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetPostUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetWishTargetUseCase
import ijiri.ijiriserver.community.post.application.port.out.CarReader
import ijiri.ijiriserver.community.post.application.port.out.InterestCarReader
import ijiri.ijiriserver.community.post.application.port.out.MemberReader
import ijiri.ijiriserver.community.post.application.port.out.PartReader
import ijiri.ijiriserver.community.post.application.port.out.PostPartPort
import ijiri.ijiriserver.community.post.application.port.out.PostPort
import ijiri.ijiriserver.community.post.application.port.out.PostWishReader
import ijiri.ijiriserver.community.post.entity.Post
import ijiri.ijiriserver.community.post.entity.PostPart
import ijiri.ijiriserver.community.post.entity.PostStatus
import ijiri.ijiriserver.community.post.exception.PostStatusCode
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postPort: PostPort,
    private val postPartPort: PostPartPort,
    private val memberReader: MemberReader,
    private val carReader: CarReader,
    private val interestCarReader: InterestCarReader,
    private val partReader: PartReader,
    private val getHiddenMemberIdsUseCase: GetHiddenMemberIdsUseCase,
    private val postWishReader: PostWishReader,
) : GetPostUseCase,
    GetFeedUseCase,
    GetMemberPostsUseCase,
    GetPostAuthorIdUseCase,
    GetWishTargetUseCase,
    GetPostPartSummariesUseCase,
    CountMemberPostsUseCase {

    // 숨김 처리된 게시물은 작성자 본인만 볼 수 있고, 차단 관계인 회원의 게시물은 없는 것처럼 응답한다
    override fun getPost(viewerId: Long?, postId: Long): PostResponse {
        val post = postPort.findById(postId)
            ?.takeIf { it.isPublic() || (it.isWrittenBy(viewerId) && it.status == PostStatus.HIDDEN) }
            ?: throw CustomException(PostStatusCode.POST_NOT_FOUND)
        if (viewerId != null &&
            getHiddenMemberIdsUseCase.getHiddenMemberIds(viewerId).contains(post.memberId)
        ) {
            throw CustomException(PostStatusCode.POST_NOT_FOUND)
        }
        val author = memberReader.findActive(post.memberId)
        val postPartIds = post.parts.map { it.id }
        return PostResponse.detail(
            post,
            author,
            carReader.getSpecByTrim(post.carTrimId),
            partReader.getPartInfos(distinctPartIds(post.parts)),
            postWishReader.countWishes(postPartIds),
            if (viewerId != null) postWishReader.findWishedPostPartIds(viewerId, postPartIds) else emptySet(),
            post.isWrittenBy(viewerId),
        )
    }

    override fun getFeed(
        viewerId: Long?,
        carModelId: Long?,
        carModelIds: List<Long>,
        buildStyle: BuildStyle?,
        cursor: Long?,
        size: Int,
    ): PostResponse {
        val models = resolveFeedCarModels(viewerId, carModelId, carModelIds)
        val hiddenAuthors = hiddenAuthorsFor(viewerId)
        val spec = Specification<Post> { root, _, cb ->
            cb.and(
                cb.equal(root.get<PostStatus>("status"), PostStatus.PUBLIC),
                if (models.isEmpty()) cb.conjunction() else root.get<Long>("carModelId").`in`(models),
                if (buildStyle != null) cb.equal(root.get<BuildStyle>("buildStyle"), buildStyle) else cb.conjunction(),
                if (hiddenAuthors.isEmpty()) {
                    cb.conjunction()
                } else {
                    cb.not(root.get<Long>("memberId").`in`(hiddenAuthors))
                },
                if (cursor != null) cb.lessThan(root.get<Long>("id"), cursor) else cb.conjunction(),
            )
        }
        return toCards(spec, size)
    }

    // 내 게시물 목록에는 숨김 처리된 게시물도 hidden 표시와 함께 보여준다
    override fun getMemberPosts(viewerId: Long?, memberId: Long, cursor: Long?, size: Int): PostResponse {
        if (hiddenAuthorsFor(viewerId).contains(memberId)) {
            return PostResponse.cards(emptyList(), null)
        }
        val mine = memberId == viewerId
        val spec = Specification<Post> { root, _, cb ->
            cb.and(
                cb.equal(root.get<Long>("memberId"), memberId),
                if (mine) {
                    cb.notEqual(root.get<PostStatus>("status"), PostStatus.AUTHOR_WITHDRAWN)
                } else {
                    cb.equal(root.get<PostStatus>("status"), PostStatus.PUBLIC)
                },
                if (cursor != null) cb.lessThan(root.get<Long>("id"), cursor) else cb.conjunction(),
            )
        }
        return toCards(spec, size)
    }

    override fun getAuthorId(postId: Long): Long =
        postPort.findById(postId)?.memberId
            ?: throw CustomException(PostStatusCode.POST_NOT_FOUND)

    override fun getWishTarget(postPartId: Long): WishTarget {
        val part = postPartPort.findById(postPartId)
            ?.takeIf { it.post.isPublic() }
            ?: throw CustomException(PostStatusCode.POST_NOT_FOUND)
        return WishTarget(part.id, part.post.id, part.post.memberId)
    }

    override fun getPostPartSummaries(postPartIds: Collection<Long>): Map<Long, PostPartSummary> {
        val parts = postPartPort.findAllById(postPartIds)
            .filter { it.post.isPublic() }
        val infos = partReader.getPartInfos(distinctPartIds(parts))
        val modelNames = carReader.getModelNames(parts.map { it.post.carModelId }.toSet())
        return parts.associate { part ->
            val post = part.post
            val info = infos[part.partId]
            part.id to PostPartSummary(
                part.id,
                post.id,
                post.thumbnail?.url,
                part.category,
                info?.brandName,
                info?.name,
                modelNames[post.carModelId],
            )
        }
    }

    override fun countVisiblePosts(memberId: Long): Long =
        postPort.countByMemberIdAndStatus(memberId, PostStatus.PUBLIC)

    override fun countPostsByOwnedCar(ownedCarIds: Collection<Long>): Map<Long, Long> {
        if (ownedCarIds.isEmpty()) {
            return emptyMap()
        }
        return postPort.countByOwnedCarIds(ownedCarIds)
            .associate { row -> row[0] as Long to row[1] as Long }
    }

    private fun resolveFeedCarModels(viewerId: Long?, carModelId: Long?, carModelIds: List<Long>): List<Long> {
        if (carModelId != null) {
            return listOf(carModelId)
        }
        if (carModelIds.isNotEmpty()) {
            return carModelIds
        }
        return if (viewerId != null) interestCarReader.getCarModelIds(viewerId) else emptyList()
    }

    private fun toCards(spec: Specification<Post>, size: Int): PostResponse {
        val found = postPort.findNewest(spec, size + 1)
        val hasNext = found.size > size
        val page = if (hasNext) found.subList(0, size) else found
        val modelNames = carReader.getModelNames(page.map { it.carModelId }.toSet())
        return PostResponse.cards(
            page.map { PostResponse.Card.of(it, modelNames[it.carModelId]) },
            if (hasNext) page.last().id else null,
        )
    }

    private fun hiddenAuthorsFor(viewerId: Long?): Set<Long> =
        if (viewerId != null) getHiddenMemberIdsUseCase.getHiddenMemberIds(viewerId) else emptySet()

    private fun distinctPartIds(parts: List<PostPart>): Set<Long> = parts.map { it.partId }.toSet()
}
