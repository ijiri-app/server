package ijiri.ijiriserver.domain.post.event;

/**
 * 게시물이 삭제될 때(작성자 삭제, 회원 영구 삭제) 같은 트랜잭션에서 발행. 위시리스트가 이 게시물의 담기를 지운다.
 */
public record PostDeletedEvent(
        Long postId
) {
}
