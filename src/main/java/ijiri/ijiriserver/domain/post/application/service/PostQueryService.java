package ijiri.ijiriserver.domain.post.application.service;

import ijiri.ijiriserver.domain.block.application.port.in.GetHiddenMemberIdsUseCase;
import ijiri.ijiriserver.domain.carmodel.application.port.in.GetCarModelNamesUseCase;
import ijiri.ijiriserver.domain.carmodel.application.port.in.GetCarSpecByTrimUseCase;
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.interestcar.application.port.in.GetInterestCarModelIdsUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.FindActiveMemberUseCase;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.part.application.data.PartInfo;
import ijiri.ijiriserver.domain.part.application.port.in.GetPartInfosUseCase;
import ijiri.ijiriserver.domain.post.application.data.PostPartSummary;
import ijiri.ijiriserver.domain.post.application.data.WishTarget;
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse;
import ijiri.ijiriserver.domain.post.application.port.in.GetFeedUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.GetMemberPostsUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.GetPostAuthorIdUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.GetPostPartSummariesUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.GetPostUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.GetWishTargetUseCase;
import ijiri.ijiriserver.domain.post.application.port.out.PostPartPort;
import ijiri.ijiriserver.domain.post.application.port.out.PostPort;
import ijiri.ijiriserver.domain.post.application.port.out.PostWishReader;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostPart;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import ijiri.ijiriserver.domain.post.exception.PostStatusCode;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService implements GetPostUseCase,
        GetFeedUseCase,
        GetMemberPostsUseCase,
        GetPostAuthorIdUseCase,
        GetWishTargetUseCase,
        GetPostPartSummariesUseCase {

    private final PostPort postPort;
    private final PostPartPort postPartPort;
    private final FindActiveMemberUseCase findActiveMemberUseCase;
    private final GetCarModelNamesUseCase getCarModelNamesUseCase;
    private final GetCarSpecByTrimUseCase getCarSpecByTrimUseCase;
    private final GetInterestCarModelIdsUseCase getInterestCarModelIdsUseCase;
    private final GetPartInfosUseCase getPartInfosUseCase;
    private final GetHiddenMemberIdsUseCase getHiddenMemberIdsUseCase;
    private final PostWishReader postWishReader;

    // 숨김 처리된 게시물은 작성자 본인만 볼 수 있고, 차단 관계인 회원의 게시물은 없는 것처럼 응답한다
    @Override
    public PostResponse getPost(Long viewerId, Long postId) {
        Post post = postPort.findById(postId)
                .filter(found -> found.isPublic()
                        || (found.isWrittenBy(viewerId) && found.getStatus() == PostStatus.HIDDEN))
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        if (viewerId != null
                && getHiddenMemberIdsUseCase.getHiddenMemberIds(viewerId).contains(post.getMemberId())) {
            throw new CustomException(PostStatusCode.POST_NOT_FOUND);
        }
        Member author = findActiveMemberUseCase.findActiveMember(post.getMemberId()).orElse(null);
        List<Long> postPartIds = post.getParts().stream().map(PostPart::getId).toList();
        return PostResponse.detail(
                post,
                author,
                getCarSpecByTrimUseCase.getSpecByTrim(post.getCarTrimId()),
                getPartInfosUseCase.getPartInfos(distinctPartIds(post.getParts())),
                postWishReader.countWishes(postPartIds),
                viewerId != null ? postWishReader.findWishedPostPartIds(viewerId, postPartIds) : Set.of(),
                post.isWrittenBy(viewerId)
        );
    }

    @Override
    public PostResponse getFeed(
            Long viewerId,
            Long carModelId,
            List<Long> carModelIds,
            BuildStyle buildStyle,
            Long cursor,
            int size
    ) {
        List<Long> models = resolveFeedCarModels(viewerId, carModelId, carModelIds);
        Set<Long> hiddenAuthors = hiddenAuthorsFor(viewerId);
        Specification<Post> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PostStatus.PUBLIC),
                models.isEmpty() ? cb.conjunction() : root.get("carModelId").in(models),
                buildStyle != null ? cb.equal(root.get("buildStyle"), buildStyle) : cb.conjunction(),
                hiddenAuthors.isEmpty() ? cb.conjunction() : cb.not(root.get("memberId").in(hiddenAuthors)),
                cursor != null ? cb.lessThan(root.get("id"), cursor) : cb.conjunction()
        );
        return toCards(spec, size);
    }

    // 내 게시물 목록에는 숨김 처리된 게시물도 hidden 표시와 함께 보여준다
    @Override
    public PostResponse getMemberPosts(Long viewerId, Long memberId, Long cursor, int size) {
        if (hiddenAuthorsFor(viewerId).contains(memberId)) {
            return PostResponse.cards(List.of(), null);
        }
        boolean mine = memberId.equals(viewerId);
        Specification<Post> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("memberId"), memberId),
                mine ? cb.notEqual(root.get("status"), PostStatus.AUTHOR_WITHDRAWN)
                        : cb.equal(root.get("status"), PostStatus.PUBLIC),
                cursor != null ? cb.lessThan(root.get("id"), cursor) : cb.conjunction()
        );
        return toCards(spec, size);
    }

    @Override
    public Long getAuthorId(Long postId) {
        return postPort.findById(postId)
                .map(Post::getMemberId)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
    }

    @Override
    public WishTarget getWishTarget(Long postPartId) {
        PostPart part = postPartPort.findById(postPartId)
                .filter(found -> found.getPost().isPublic())
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        return new WishTarget(part.getId(), part.getPost().getId(), part.getPost().getMemberId());
    }

    @Override
    public Map<Long, PostPartSummary> getPostPartSummaries(Collection<Long> postPartIds) {
        List<PostPart> parts = postPartPort.findAllById(postPartIds).stream()
                .filter(part -> part.getPost().isPublic())
                .toList();
        Map<Long, PartInfo> infos = getPartInfosUseCase.getPartInfos(distinctPartIds(parts));
        Map<Long, String> modelNames = getCarModelNamesUseCase.getModelNames(
                parts.stream().map(part -> part.getPost().getCarModelId()).collect(Collectors.toSet())
        );
        return parts.stream().collect(Collectors.toMap(PostPart::getId, part -> {
            Post post = part.getPost();
            PartInfo info = infos.get(part.getPartId());
            return new PostPartSummary(
                    part.getId(),
                    post.getId(),
                    post.getThumbnail() != null ? post.getThumbnail().getUrl() : null,
                    part.getCategory(),
                    info != null ? info.brandName() : null,
                    info != null ? info.name() : null,
                    modelNames.get(post.getCarModelId())
            );
        }));
    }

    private List<Long> resolveFeedCarModels(Long viewerId, Long carModelId, List<Long> carModelIds) {
        if (carModelId != null) {
            return List.of(carModelId);
        }
        if (!carModelIds.isEmpty()) {
            return carModelIds;
        }
        return viewerId != null ? getInterestCarModelIdsUseCase.getCarModelIds(viewerId) : List.of();
    }

    private PostResponse toCards(Specification<Post> spec, int size) {
        List<Post> found = postPort.findNewest(spec, size + 1);
        boolean hasNext = found.size() > size;
        List<Post> page = hasNext ? found.subList(0, size) : found;
        Map<Long, String> modelNames = getCarModelNamesUseCase.getModelNames(
                page.stream().map(Post::getCarModelId).collect(Collectors.toSet())
        );
        return PostResponse.cards(
                page.stream()
                        .map(post -> PostResponse.Card.of(post, modelNames.get(post.getCarModelId())))
                        .toList(),
                hasNext ? page.getLast().getId() : null
        );
    }

    private Set<Long> hiddenAuthorsFor(Long viewerId) {
        return viewerId != null ? getHiddenMemberIdsUseCase.getHiddenMemberIds(viewerId) : Set.of();
    }

    private Set<Long> distinctPartIds(List<PostPart> parts) {
        return parts.stream()
                .map(PostPart::getPartId)
                .collect(Collectors.toSet());
    }
}
