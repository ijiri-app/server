package ijiri.ijiriserver.domain.wishlist.adapter.out.persistence;

import ijiri.ijiriserver.domain.member.application.port.out.MemberWishCounter;
import ijiri.ijiriserver.domain.post.application.port.out.PostWishReader;
import ijiri.ijiriserver.domain.wishlist.application.port.out.WishlistItemPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게시물·회원 도메인에 담기 수를 제공한다. 저장소만 의존해 서비스 사이에 순환 의존이 생기지 않게 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishCountProvider implements PostWishReader, MemberWishCounter {

    private final WishlistItemPort wishlistItemPort;

    @Override
    public Map<Long, Long> countWishes(Collection<Long> postPartIds) {
        if (postPartIds.isEmpty()) {
            return Map.of();
        }
        return wishlistItemPort.countByPostPartIds(postPartIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Override
    public Set<Long> findWishedPostPartIds(Long memberId, Collection<Long> postPartIds) {
        if (postPartIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(wishlistItemPort.findPostPartIds(memberId, postPartIds));
    }

    @Override
    public long countReceivedWishes(Long memberId) {
        return wishlistItemPort.countReceived(memberId);
    }
}
