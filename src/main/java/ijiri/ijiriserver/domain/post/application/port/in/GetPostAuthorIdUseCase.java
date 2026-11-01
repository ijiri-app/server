package ijiri.ijiriserver.domain.post.application.port.in;

public interface GetPostAuthorIdUseCase {

    /**
     * 신고 대상 확인용. 없는 게시물이면 NOT_FOUND.
     */
    Long getAuthorId(Long postId);
}
