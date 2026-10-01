package ijiri.ijiriserver.domain.post.service;

import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import ijiri.ijiriserver.domain.post.dto.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostUpdateRequest;
import ijiri.ijiriserver.domain.post.dto.response.PostResponse;

import java.util.List;

public interface PostService {

    PostResponse create(Long memberId, PostCreateRequest request);

    PostResponse update(Long memberId, Long postId, PostUpdateRequest request);

    PostResponse delete(Long memberId, Long postId);

    /**
     * viewerId 는 비로그인이면 null.
     */
    PostResponse getPost(Long viewerId, Long postId);

    /**
     * carModelIds 가 비어 있으면 전체 차종. viewerId 는 비로그인이면 null.
     */
    PostResponse getFeed(Long viewerId, List<Long> carModelIds, BuildDirection buildDirection, Long cursor, int size);

    PostResponse getMemberPosts(Long viewerId, Long memberId, Long cursor, int size);

    /**
     * 신고 대상 확인용. 없는 게시물이면 POST404.
     */
    Long getAuthorId(Long postId);

    void hide(Long postId);
}
