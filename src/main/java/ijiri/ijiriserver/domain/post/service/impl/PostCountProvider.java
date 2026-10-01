package ijiri.ijiriserver.domain.post.service.impl;

import ijiri.ijiriserver.domain.member.service.MemberPostCounter;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarPostCounter;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import ijiri.ijiriserver.domain.post.repository.PostRepository;
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

    private final PostRepository postRepository;

    @Override
    public long countVisiblePosts(Long memberId) {
        return postRepository.countByMemberIdAndStatus(memberId, PostStatus.PUBLIC);
    }

    @Override
    public Map<Long, Long> countPosts(Collection<Long> ownedCarIds) {
        if (ownedCarIds.isEmpty()) {
            return Map.of();
        }
        return postRepository.countByOwnedCarIds(ownedCarIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
