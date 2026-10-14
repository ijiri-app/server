package ijiri.ijiriserver.domain.post.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 게시물 상세의 담기 수와 담기 여부. 위시리스트 도메인이 게시물에 의존하므로, 반대 방향 의존이 생기지 않도록
 * 위시리스트 도메인이 구현한다.
 */
public interface PostWishReader {

    /**
     * 부품별 담기 수. 게시물 작성자 본인과 탈퇴한 회원의 담기는 세지 않는다. 담기가 없는 부품은 빠진다.
     */
    Map<Long, Long> countWishes(Collection<Long> postPartIds);

    Set<Long> findWishedPostPartIds(Long memberId, Collection<Long> postPartIds);
}
