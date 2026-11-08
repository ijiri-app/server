package ijiri.ijiriserver.community.wishlist.application.service

import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.community.post.application.port.`in`.GetPostPartSummariesUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.GetWishTargetUseCase
import ijiri.ijiriserver.community.post.event.PostDeletedEvent
import ijiri.ijiriserver.community.post.event.PostPartsRemovedEvent
import ijiri.ijiriserver.community.wishlist.application.data.WishlistAddResult
import ijiri.ijiriserver.community.wishlist.application.data.response.WishlistResponse
import ijiri.ijiriserver.community.wishlist.application.port.`in`.AddWishlistItemUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.CountReceivedWishesUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.DeleteMemberWishesUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.GetWishlistUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.HideMemberWishesUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.RemoveWishlistItemUseCase
import ijiri.ijiriserver.community.wishlist.application.port.out.WishlistItemPort
import ijiri.ijiriserver.community.wishlist.entity.WishlistItem
import ijiri.ijiriserver.community.wishlist.exception.WishlistStatusCode
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WishlistService(
    private val wishlistItemPort: WishlistItemPort,
    private val getPostPartSummariesUseCase: GetPostPartSummariesUseCase,
    private val getWishTargetUseCase: GetWishTargetUseCase,
) : AddWishlistItemUseCase,
    RemoveWishlistItemUseCase,
    GetWishlistUseCase,
    HideMemberWishesUseCase,
    DeleteMemberWishesUseCase,
    CountReceivedWishesUseCase {

    // 비로그인 상태에서 담기를 누르고 로그인한 뒤 다시 보내는 흐름이 있으므로, 이미 담긴 부품이면 기존 항목을 돌려준다
    @Transactional
    override fun add(memberId: Long, postPartId: Long): WishlistAddResult {
        val target = getWishTargetUseCase.getWishTarget(postPartId)
        wishlistItemPort.findByMemberIdAndPostPartId(memberId, postPartId)?.let {
            return WishlistAddResult(it.id, false)
        }
        val saved = wishlistItemPort.save(
            WishlistItem(
                memberId = memberId,
                postPartId = target.postPartId,
                postId = target.postId,
                postAuthorId = target.postAuthorId,
            )
        )
        return WishlistAddResult(saved.id, true)
    }

    @Transactional
    override fun remove(memberId: Long, wishlistItemId: Long) {
        val item = wishlistItemPort.findByIdAndMemberId(wishlistItemId, memberId)
            ?: throw CustomException(WishlistStatusCode.WISHLIST_ITEM_NOT_FOUND)
        wishlistItemPort.delete(item)
    }

    // 숨김 처리되거나 작성자가 탈퇴한 게시물의 부품은 목록에서 뺀다
    override fun getWishlist(memberId: Long, cursor: Long?, size: Int): WishlistResponse {
        val found = wishlistItemPort.findByMemberIdAndIdLessThanOrderByIdDesc(
            memberId,
            cursor ?: Long.MAX_VALUE,
            Limit.of(size + 1),
        )
        val hasNext = found.size > size
        val page = if (hasNext) found.subList(0, size) else found
        val parts = getPostPartSummariesUseCase.getPostPartSummaries(
            page.map { it.postPartId }
        )
        return WishlistResponse.list(
            page.mapNotNull { item -> parts[item.postPartId]?.let { WishlistResponse.Item.of(item, it) } },
            if (hasNext) page.last().id else null,
        )
    }

    override fun countReceived(memberId: Long): Long = wishlistItemPort.countReceived(memberId)

    @EventListener
    @Transactional
    fun removeAll(event: PostDeletedEvent) {
        wishlistItemPort.deleteAllByPostId(event.postId)
    }

    @EventListener
    @Transactional
    fun removeAll(event: PostPartsRemovedEvent) {
        wishlistItemPort.deleteAllByPostPartIds(event.postPartIds)
    }

    // 탈퇴한 회원의 담기는 담기 수에서 빠진다. 행은 영구 삭제 때 지운다
    @Transactional
    override fun hideAll(memberId: Long) {
        wishlistItemPort.hideAllByMemberId(memberId)
    }

    @Transactional
    override fun deleteAll(memberId: Long) {
        wishlistItemPort.deleteAllByMemberIdInBulk(memberId)
    }
}
