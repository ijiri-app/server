package ijiri.ijiriserver.domain.post.event;

import java.util.List;

/**
 * 게시물 수정으로 부품이 빠질 때 같은 트랜잭션에서 발행. 위시리스트가 그 부품의 담기를 지운다.
 */
public record PostPartsRemovedEvent(
        List<Long> postPartIds
) {
}
