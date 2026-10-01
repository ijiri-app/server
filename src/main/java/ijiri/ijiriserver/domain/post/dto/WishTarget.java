package ijiri.ijiriserver.domain.post.dto;

/**
 * 위시리스트에 담을 수 있는(공개 게시물의) 부품.
 */
public record WishTarget(
        Long postPartId,
        Long postId,
        Long postAuthorId
) {
}
