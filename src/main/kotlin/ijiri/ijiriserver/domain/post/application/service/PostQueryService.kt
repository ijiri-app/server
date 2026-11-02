package ijiri.ijiriserver.domain.post.application.service

import ijiri.ijiriserver.domain.block.application.port.`in`.GetHiddenMemberIdsUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelNamesUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarSpecByTrimUseCase
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle
import ijiri.ijiriserver.domain.interestcar.application.port.`in`.GetInterestCarModelIdsUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.GetPartInfosUseCase
import ijiri.ijiriserver.domain.post.application.data.PostPartSummary
import ijiri.ijiriserver.domain.post.application.data.WishTarget
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse
import ijiri.ijiriserver.domain.post.application.port.`in`.GetFeedUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetMemberPostsUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetPostAuthorIdUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetPostPartSummariesUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetPostUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetWishTargetUseCase
import ijiri.ijiriserver.domain.post.application.port.out.PostPartPort
import ijiri.ijiriserver.domain.post.application.port.out.PostPort
import ijiri.ijiriserver.domain.post.application.port.out.PostWishReader
import ijiri.ijiriserver.domain.post.entity.Post
import ijiri.ijiriserver.domain.post.entity.PostPart
import ijiri.ijiriserver.domain.post.entity.PostStatus
import ijiri.ijiriserver.domain.post.exception.PostStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postPort: PostPort,
    private val postPartPort: PostPartPort,
    private val findActiveMemberUseCase: FindActiveMemberUseCase,
    private val getCarModelNamesUseCase: GetCarModelNamesUseCase,
    private val getCarSpecByTrimUseCase: GetCarSpecByTrimUseCase,
    private val getInterestCarModelIdsUseCase: GetInterestCarModelIdsUseCase,
    private val getPartInfosUseCase: GetPartInfosUseCase,
    private val getHiddenMemberIdsUseCase: GetHiddenMemberIdsUseCase,
    private val postWishReader: PostWishReader,
) : GetPostUseCase,
    GetFeedUseCase,
    GetMemberPostsUseCase,
    GetPostAuthorIdUseCase,
    GetWishTargetUseCase,
    GetPostPartSummariesUseCase {

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
        val author = findActiveMemberUseCase.findActiveMember(post.memberId)
        val postPartIds = post.parts.map { it.id }
        return PostResponse.detail(
            post,
            author,
            getCarSpecByTrimUseCase.getSpecByTrim(post.carTrimId),
            getPartInfosUseCase.getPartInfos(distinctPartIds(post.parts)),
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
        val infos = getPartInfosUseCase.getPartInfos(distinctPartIds(parts))
        val modelNames = getCarModelNamesUseCase.getModelNames(
            parts.map { it.post.carModelId }.toSet()
        )
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

    private fun resolveFeedCarModels(viewerId: Long?, carModelId: Long?, carModelIds: List<Long>): List<Long> {
        if (carModelId != null) {
            return listOf(carModelId)
        }
        if (carModelIds.isNotEmpty()) {
            return carModelIds
        }
        return if (viewerId != null) getInterestCarModelIdsUseCase.getCarModelIds(viewerId) else emptyList()
    }

    private fun toCards(spec: Specification<Post>, size: Int): PostResponse {
        val found = postPort.findNewest(spec, size + 1)
        val hasNext = found.size > size
        val page = if (hasNext) found.subList(0, size) else found
        val modelNames = getCarModelNamesUseCase.getModelNames(
            page.map { it.carModelId }.toSet()
        )
        return PostResponse.cards(
            page.map { PostResponse.Card.of(it, modelNames[it.carModelId]) },
            if (hasNext) page.last().id else null,
        )
    }

    private fun hiddenAuthorsFor(viewerId: Long?): Set<Long> =
        if (viewerId != null) getHiddenMemberIdsUseCase.getHiddenMemberIds(viewerId) else emptySet()

    private fun distinctPartIds(parts: List<PostPart>): Set<Long> = parts.map { it.partId }.toSet()
}
