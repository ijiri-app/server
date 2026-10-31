package ijiri.ijiriserver.domain.post.adapter.out.persistence;

import ijiri.ijiriserver.domain.member.application.port.out.MemberPostCounter;
import ijiri.ijiriserver.domain.ownedcar.application.port.out.OwnedCarPostCounter;
import ijiri.ijiriserver.domain.post.application.port.out.PostPort;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 회원·보유 차량 도메인에 게시물 수를 제공한다. 저장소만 의존해 서비스 사이에 순환 의존이 생기지 않게 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCountProvider implements MemberPostCounter, OwnedCarPostCounter {

    private final PostPort postPort;

    @Override
    public long countVisiblePosts(Long memberId) {
        return postPort.countByMemberIdAndStatus(memberId, PostStatus.PUBLIC);
    }

    @Override
    public Map<Long, Long> countPosts(Collection<Long> ownedCarIds) {
        if (ownedCarIds.isEmpty()) {
            return Map.of();
        }
        return postPort.countByOwnedCarIds(ownedCarIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
