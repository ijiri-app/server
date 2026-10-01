package ijiri.ijiriserver.domain.ownedcar.service;

import java.util.Collection;
import java.util.Map;

/**
 * 보유 차량별 게시물 수. 게시물 도메인이 보유 차량에 의존하므로, 반대 방향 의존이 생기지 않도록 게시물 도메인이 구현한다.
 */
public interface OwnedCarPostCounter {

    /**
     * 게시물이 없는 차량은 결과에서 빠진다.
     */
    Map<Long, Long> countPosts(Collection<Long> ownedCarIds);
}
