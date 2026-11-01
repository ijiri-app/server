package ijiri.ijiriserver.domain.post.application.port.in;

import ijiri.ijiriserver.domain.post.application.data.response.PostResponse;

public interface GetPostUseCase {

    /**
     * viewerId 는 비로그인이면 null.
     */
    PostResponse getPost(Long viewerId, Long postId);
}
