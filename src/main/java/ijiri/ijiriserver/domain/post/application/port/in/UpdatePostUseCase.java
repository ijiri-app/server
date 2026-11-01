package ijiri.ijiriserver.domain.post.application.port.in;

import ijiri.ijiriserver.domain.post.application.data.request.PostUpdateRequest;

public interface UpdatePostUseCase {

    void update(Long memberId, Long postId, PostUpdateRequest request);
}
