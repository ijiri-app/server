package ijiri.ijiriserver.domain.post.service;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.post.dto.PostPartSummary;
import ijiri.ijiriserver.domain.post.dto.WishTarget;
import ijiri.ijiriserver.domain.post.dto.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostUpdateRequest;
import ijiri.ijiriserver.domain.post.dto.response.PostResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PostService {

    PostResponse create(Long memberId, PostCreateRequest request);

    void update(Long memberId, Long postId, PostUpdateRequest request);

    void delete(Long memberId, Long postId);

    /**
     * viewerId 는 비로그인이면 null.
     */
    PostResponse getPost(Long viewerId, Long postId);

    /**
     * carModelId 가 있으면 그 차종, 없으면 carModelIds, 그것도 없으면 로그인한 회원의 관심 차종 전체.
     * 관심 차종도 없으면 전체 게시물. viewerId 는 비로그인이면 null.
     */
    PostResponse getFeed(
            Long viewerId,
            Long carModelId,
            List<Long> carModelIds,
            BuildStyle buildStyle,
            Long cursor,
            int size
    );

    PostResponse getMemberPosts(Long viewerId, Long memberId, Long cursor, int size);

    /**
     * 신고 대상 확인용. 없는 게시물이면 NOT_FOUND.
     */
    Long getAuthorId(Long postId);

    /**
     * 이미 삭제된 게시물이면 아무것도 하지 않는다 (신고 처리가 막히지 않도록).
     */
    void hide(Long postId);

    /**
     * 공개 게시물의 부품이 아니면 NOT_FOUND.
     */
    WishTarget getWishTarget(Long postPartId);

    /**
     * 공개 게시물의 부품만 돌려준다 (숨김·삭제된 게시물은 빠진다).
     */
    Map<Long, PostPartSummary> getPostPartSummaries(Collection<Long> postPartIds);
}
