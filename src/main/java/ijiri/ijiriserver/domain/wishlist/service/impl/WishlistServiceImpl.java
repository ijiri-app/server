package ijiri.ijiriserver.domain.wishlist.service.impl;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.exception.PartStatusCode;
import ijiri.ijiriserver.domain.part.service.PartService;
import ijiri.ijiriserver.domain.wishlist.dto.response.WishlistResponse;
import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem;
import ijiri.ijiriserver.domain.wishlist.exception.WishlistStatusCode;
import ijiri.ijiriserver.domain.wishlist.repository.WishlistItemRepository;
import ijiri.ijiriserver.domain.wishlist.service.WishlistService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final PartService partService;
    private final MemberService memberService;

    // 비로그인 상태에서 담기를 누르고 로그인한 뒤 다시 보내는 흐름이 있으므로, 이미 담긴 부품이면 기존 항목을 돌려준다
    @Override
    @Transactional
    public WishlistResponse add(Long memberId, Long partId) {
        memberService.getById(memberId);
        PartInfo part = partService.getPartInfos(List.of(partId)).get(partId);
        if (part == null) {
            throw new CustomException(PartStatusCode.PART_NOT_FOUND);
        }
        WishlistItem item = wishlistItemRepository.findByMemberIdAndPartId(memberId, partId)
                .orElseGet(() -> wishlistItemRepository.save(WishlistItem.builder()
                        .memberId(memberId)
                        .partId(partId)
                        .build()
                ));
        return WishlistResponse.single(WishlistResponse.Item.of(item, part));
    }

    @Override
    @Transactional
    public WishlistResponse remove(Long memberId, Long wishlistItemId) {
        WishlistItem item = wishlistItemRepository.findByIdAndMemberId(wishlistItemId, memberId)
                .orElseThrow(() -> new CustomException(WishlistStatusCode.WISHLIST_ITEM_NOT_FOUND));
        wishlistItemRepository.delete(item);
        return WishlistResponse.message(WishlistStatusCode.REMOVE_SUCCESS.getMessage());
    }

    @Override
    public WishlistResponse getWishlist(Long memberId, Long cursor, int size) {
        List<WishlistItem> found = wishlistItemRepository.findByMemberIdAndIdLessThanOrderByIdDesc(
                memberId,
                cursor != null ? cursor : Long.MAX_VALUE,
                Limit.of(size + 1)
        );
        boolean hasNext = found.size() > size;
        List<WishlistItem> page = hasNext ? found.subList(0, size) : found;
        Map<Long, PartInfo> parts = partService.getPartInfos(page.stream().map(WishlistItem::getPartId).toList());
        return WishlistResponse.list(
                page.stream()
                        .map(item -> WishlistResponse.Item.of(item, parts.get(item.getPartId())))
                        .toList(),
                hasNext ? page.getLast().getId() : null
        );
    }

    @Override
    public Set<Long> getWishlistedPartIds(Long memberId, Collection<Long> partIds) {
        if (partIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(wishlistItemRepository.findPartIds(memberId, partIds));
    }

    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        wishlistItemRepository.deleteAllByMemberIdInBulk(event.memberId());
    }
}
