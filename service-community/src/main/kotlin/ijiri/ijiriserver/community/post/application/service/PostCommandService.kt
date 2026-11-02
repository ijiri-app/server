package ijiri.ijiriserver.community.post.application.service

import ijiri.ijiriserver.common.contract.PartInfo
import ijiri.ijiriserver.common.contract.PartResolveRequest
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.transaction.TransactionHooks
import ijiri.ijiriserver.community.post.application.data.request.PostCreateRequest
import ijiri.ijiriserver.community.post.application.data.request.PostImageRequest
import ijiri.ijiriserver.community.post.application.data.request.PostPartRequest
import ijiri.ijiriserver.community.post.application.data.request.PostUpdateRequest
import ijiri.ijiriserver.community.post.application.data.response.PostResponse
import ijiri.ijiriserver.community.post.application.port.`in`.CreatePostUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.DeleteMemberPostsUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.DeletePostUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.HideMemberPostsUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.HidePostUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.UpdatePostUseCase
import ijiri.ijiriserver.community.post.application.port.out.ImageAttacher
import ijiri.ijiriserver.community.post.application.port.out.OwnedCarReader
import ijiri.ijiriserver.community.post.application.port.out.PartReader
import ijiri.ijiriserver.community.post.application.port.out.PostEventPublisher
import ijiri.ijiriserver.community.post.application.port.out.PostPort
import ijiri.ijiriserver.community.post.entity.Post
import ijiri.ijiriserver.community.post.entity.PostImage
import ijiri.ijiriserver.community.post.entity.PostPart
import ijiri.ijiriserver.community.post.entity.PostStatus
import ijiri.ijiriserver.community.post.event.PostDeletedEvent
import ijiri.ijiriserver.community.post.event.PostPartsRemovedEvent
import ijiri.ijiriserver.community.post.exception.PostStatusCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostCommandService(
    private val postPort: PostPort,
    private val ownedCarReader: OwnedCarReader,
    private val partReader: PartReader,
    private val imageAttacher: ImageAttacher,
    private val postEventPublisher: PostEventPublisher,
    private val eventPublisher: ApplicationEventPublisher,
) : CreatePostUseCase,
    UpdatePostUseCase,
    DeletePostUseCase,
    HidePostUseCase,
    HideMemberPostsUseCase,
    DeleteMemberPostsUseCase {

    // 보유 차량은 service-user, 사진은 service-media, 부품·브랜드는 service-car 가 확인·등록한다 (한 번의 요청으로 작성 완료).
    // 게이트웨이가 세션을 확인하므로 회원이 활성인지 다시 보지 않는다
    @Transactional
    override fun create(memberId: Long, request: PostCreateRequest): PostResponse {
        val car = ownedCarReader.getSnapshot(memberId, request.ownedCarId!!)
        val imageRequests = request.images!!
        if (imageRequests.any { it.width == null || it.height == null }) {
            throw CustomException(PostStatusCode.MISSING_IMAGE_SIZE)
        }
        val post = Post.of(memberId, car, request.buildStyle!!, normalizeContent(request.content))
        val attached = imageAttacher.attach(memberId, imageRequests.map { it.imageKey!! })
        // 저장소의 영구 키 이동은 DB 롤백으로 되돌릴 수 없으므로, 롤백되면 옮겨진 사진을 지워 같은 업로드 키로 다시 시도할 수 있게 한다
        TransactionHooks.afterRollback { imageAttacher.delete(memberId, attached.map { it.imageKey }) }
        // 태그는 업로드 때 받은 키로 사진을 가리키지만, 연결하면서 사진은 영구 키로 옮겨진다
        val imagesByRequestKey = HashMap<String, PostImage>()
        attached.forEachIndexed { order, attachedImage ->
            val image = imageRequests[order]
            val postImage = PostImage(
                post = post,
                imageKey = attachedImage.imageKey,
                url = attachedImage.url,
                width = image.width!!,
                height = image.height!!,
                displayOrder = order,
            )
            post.addImage(postImage)
            imagesByRequestKey[image.imageKey!!] = postImage
        }
        post.replaceParts(buildParts(post, request.parts.orEmpty(), imageRequests, imagesByRequestKey, emptyMap()))
        postPort.save(post)
        publishUsage(post.carModelId, distinctPartIds(post.parts), emptySet())
        return PostResponse.created(post.id)
    }

    // 부품을 바꾸면 빠진 부품의 담기를 지우고, 부품별 사용 수를 다시 맞춘다
    @Transactional
    override fun update(memberId: Long, postId: Long, request: PostUpdateRequest) {
        // 사진은 바꿀 수 없고 images 는 parts 의 태그를 싣는 용도라, parts 없이 오면 조용히 무시하지 않고 거절한다
        if (request.parts == null && request.images != null) {
            throw CustomException(PostStatusCode.TAGS_WITHOUT_PARTS)
        }
        val post = getOwnPost(memberId, postId)
        if (request.buildStyle != null) {
            post.changeBuildStyle(request.buildStyle)
        }
        if (request.content != null) {
            post.changeContent(normalizeContent(request.content))
        }
        if (request.parts == null) {
            return
        }
        val existing = post.parts.associateBy { it.id }
        val beforePartIds = distinctPartIds(post.parts)
        val parts = buildParts(
            post,
            request.parts,
            request.images.orEmpty(),
            post.images.associateBy { it.imageKey },
            existing,
        )
        val removed = existing.keys.filter { id -> parts.none { it.id == id } }
        post.replaceParts(parts)

        val afterPartIds = distinctPartIds(parts)
        publishUsage(post.carModelId, afterPartIds - beforePartIds, beforePartIds - afterPartIds)
        if (removed.isNotEmpty()) {
            eventPublisher.publishEvent(PostPartsRemovedEvent(removed))
        }
    }

    @Transactional
    override fun delete(memberId: Long, postId: Long) {
        deletePost(getOwnPost(memberId, postId))
    }

    @Transactional
    override fun hide(postId: Long) {
        postPort.findById(postId)?.hide()
    }

    @Transactional
    override fun hideAll(memberId: Long) {
        postPort.hideAllByAuthor(memberId)
    }

    // 영구 삭제. 사진 파일은 service-media 가 post.deleted 로 받아 지운다
    @Transactional
    override fun deleteAll(memberId: Long) {
        postPort.findAllByMemberId(memberId).forEach { deletePost(it) }
    }

    private fun deletePost(post: Post) {
        val postId = post.id
        val imageKeys = post.images.map { it.imageKey }
        eventPublisher.publishEvent(PostDeletedEvent(postId))
        publishUsage(post.carModelId, emptySet(), distinctPartIds(post.parts))
        // 태그는 사진을 FK 로 가리키는데 Hibernate 의 삭제 순서는 보장되지 않으므로, 부품·태그를 먼저 지운다
        post.replaceParts(emptyList())
        postPort.flush()
        postPort.delete(post)
        TransactionHooks.afterCommit { postEventPublisher.postDeleted(postId, imageKeys) }
    }

    // 사용 수는 순위용 수치라 커밋 뒤에 보낸다. 롤백된 작성·수정은 세지 않는다
    private fun publishUsage(carModelId: Long, addedPartIds: Set<Long>, removedPartIds: Set<Long>) {
        if (addedPartIds.isEmpty() && removedPartIds.isEmpty()) {
            return
        }
        TransactionHooks.afterCommit { postEventPublisher.partUsageChanged(carModelId, addedPartIds, removedPartIds) }
    }

    // ref 로 태그와 부품을 잇는다. 수정할 때 postPartId 가 있는 부품은 기존 PostPart 를 그대로 쓴다
    private fun buildParts(
        post: Post,
        partRequests: List<PostPartRequest>,
        imageRequests: List<PostImageRequest>,
        images: Map<String, PostImage>,
        existing: Map<Long, PostPart>,
    ): List<PostPart> {
        val byRef = HashMap<String, PostPart>()
        val parts = partRequests.mapIndexed { order, request ->
            val kept = request.postPartId?.let { existing[it] }
            val part = if (kept != null) {
                keepExisting(kept, request, byRef.values)
            } else {
                PostPart.of(post, resolvePart(request).id, request.category!!, order)
            }
            part.reorder(request.category!!, order)
            part.clearTags()
            if (byRef.put(request.ref!!, part) != null) {
                throw CustomException(PostStatusCode.INVALID_PART_REF)
            }
            part
        }

        for (imageRequest in imageRequests) {
            val image = images[imageRequest.imageKey!!]
            for (tag in imageRequest.tags.orEmpty()) {
                val part = byRef[tag.ref!!]
                if (image == null || part == null) {
                    throw CustomException(PostStatusCode.INVALID_PART_REF)
                }
                part.addTag(image, tag.x!!, tag.y!!)
            }
        }
        return parts
    }

    // 유지하는 부품(postPartId)은 담기 기록이 가리키는 대상이라 다른 부품으로 바꿀 수 없다.
    // 바꾸려면 postPartId 없이 새 부품으로 보내고, 같은 postPartId 를 두 번 보내면 거절한다
    private fun keepExisting(part: PostPart, request: PostPartRequest, alreadyUsed: Collection<PostPart>): PostPart {
        val changesPart = request.partId != null && request.partId != part.partId
        if (changesPart || alreadyUsed.contains(part)) {
            throw CustomException(PostStatusCode.INVALID_PART_REF)
        }
        return part
    }

    private fun resolvePart(request: PostPartRequest): PartInfo =
        partReader.resolve(
            PartResolveRequest(
                request.partId,
                request.brandName,
                request.partName,
                request.category!!,
            )
        )

    private fun getOwnPost(memberId: Long, postId: Long): Post {
        val post = postPort.findById(postId)
            ?.takeIf { it.status != PostStatus.AUTHOR_WITHDRAWN }
            ?: throw CustomException(PostStatusCode.POST_NOT_FOUND)
        if (!post.isWrittenBy(memberId)) {
            throw CustomException(PostStatusCode.NOT_POST_AUTHOR)
        }
        return post
    }

    private fun distinctPartIds(parts: List<PostPart>): Set<Long> = parts.map { it.partId }.toSet()

    private fun normalizeContent(content: String?): String? = content?.trim()?.ifEmpty { null }
}
