package ijiri.ijiriserver.domain.post.application.port.in;

import ijiri.ijiriserver.domain.post.application.data.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse;

public interface CreatePostUseCase {

    PostResponse create(Long memberId, PostCreateRequest request);
}
