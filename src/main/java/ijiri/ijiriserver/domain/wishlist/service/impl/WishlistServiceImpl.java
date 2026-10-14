package ijiri.ijiriserver.domain.wishlist.service.impl;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.post.dto.PostPartSummary;
import ijiri.ijiriserver.domain.post.dto.WishTarget;
import ijiri.ijiriserver.domain.post.event.PostDeletedEvent;
import ijiri.ijiriserver.domain.post.event.PostPartsRemovedEvent;
import ijiri.ijiriserver.domain.post.service.PostService;
import ijiri.ijiriserver.domain.wishlist.dto.WishlistAddResult;
import ijiri.ijiriserver.domain.wishlist.dto.response.WishlistResponse;
import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;
import ijiri.ijiriserver.domain.wishlist.exception.WishlistStatusCode;
import ijiri.ijiriserver.domain.wishlist.repository.WishlistItemRepository;
import ijiri.ijiriserver.domain.wishlist.service.WishlistService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final PostService postService;
    private final MemberService memberService;

    // 비로그인 상태에서 담기를 누르고 로그인한 뒤 다시 보내는 흐름이 있으므로, 이미 담긴 부품이면 기존 항목을 돌려준다
    @Override
    @Transactional
    public WishlistAddResult add(Long memberId, Long postPartId) {
        memberService.getById(memberId);
        WishTarget target = postService.getWishTarget(postPartId);
        return wishlistItemRepository.findByMemberIdAndPostPartId(memberId, postPartId)
                .map(item -> new WishlistAddResult(item.getId(), false))
                .orElseGet(() -> new WishlistAddResult(
                        wishlistItemRepository.save(WishlistItem.builder()
                                .memberId(memberId)
                                .postPartId(target.postPartId())
                                .postId(target.postId())
                                .postAuthorId(target.postAuthorId())
                                .build()
                        ).getId(),
                        true
                ));
    }

    @Override
    @Transactional
    public void remove(Long memberId, Long wishlistItemId) {
        WishlistItem item = wishlistItemRepository.findByIdAndMemberId(wishlistItemId, memberId)
                .orElseThrow(() -> new CustomException(WishlistStatusCode.WISHLIST_ITEM_NOT_FOUND));
        wishlistItemRepository.delete(item);
    }

    // 숨김 처리되거나 작성자가 탈퇴한 게시물의 부품은 목록에서 뺀다
    @Override
    public WishlistResponse getWishlist(Long memberId, Long cursor, int size) {
        List<WishlistItem> found = wishlistItemRepository.findByMemberIdAndIdLessThanOrderByIdDesc(
                memberId,
                cursor != null ? cursor : Long.MAX_VALUE,
                Limit.of(size + 1)
        );
        boolean hasNext = found.size() > size;
        List<WishlistItem> page = hasNext ? found.subList(0, size) : found;
        Map<Long, PostPartSummary> parts = postService.getPostPartSummaries(
                page.stream().map(WishlistItem::getPostPartId).toList()
        );
        return WishlistResponse.list(
                page.stream()
                        .filter(item -> parts.containsKey(item.getPostPartId()))
                        .map(item -> WishlistResponse.Item.of(item, parts.get(item.getPostPartId())))
                        .toList(),
                hasNext ? page.getLast().getId() : null
        );
    }

    @EventListener
    @Transactional
    public void removeAll(PostDeletedEvent event) {
        wishlistItemRepository.deleteAllByPostId(event.postId());
    }

    @EventListener
    @Transactional
    public void removeAll(PostPartsRemovedEvent event) {
        wishlistItemRepository.deleteAllByPostPartIds(event.postPartIds());
    }

    // 탈퇴한 회원의 담기는 담기 수에서 빠진다. 행은 영구 삭제 때 지운다
    @EventListener
    @Transactional
    public void hideAll(MemberWithdrawnEvent event) {
        wishlistItemRepository.hideAllByMemberId(event.memberId());
    }

    // 영구 삭제 순서의 첫 단계 (위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원)
    @Order(0)
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        wishlistItemRepository.deleteAllByMemberIdInBulk(event.memberId());
    }
}
