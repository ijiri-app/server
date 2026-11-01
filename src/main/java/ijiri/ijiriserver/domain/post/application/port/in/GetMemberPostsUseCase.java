package ijiri.ijiriserver.domain.post.application.port.in;

import ijiri.ijiriserver.domain.post.application.data.response.PostResponse;

public interface GetMemberPostsUseCase {

    PostResponse getMemberPosts(Long viewerId, Long memberId, Long cursor, int size);
}
