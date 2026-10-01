package ijiri.ijiriserver.domain.post.service.impl;

import ijiri.ijiriserver.domain.block.service.BlockService;
import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import ijiri.ijiriserver.domain.carmodel.service.CarModelService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.ownedcar.dto.OwnedCarSnapshot;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarService;
import ijiri.ijiriserver.domain.part.dto.PartCommand;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.service.PartService;
import ijiri.ijiriserver.domain.post.dto.request.PartTagRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostImageRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostUpdateRequest;
import ijiri.ijiriserver.domain.post.dto.response.PostResponse;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostImage;
import ijiri.ijiriserver.domain.post.entity.PostPartTag;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import ijiri.ijiriserver.domain.post.exception.PostStatusCode;
import ijiri.ijiriserver.domain.post.repository.PostRepository;
import ijiri.ijiriserver.domain.post.service.PostService;
import ijiri.ijiriserver.domain.upload.dto.ImageInfo;
import ijiri.ijiriserver.domain.upload.service.UploadService;
import ijiri.ijiriserver.domain.wishlist.service.WishlistService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final MemberService memberService;
    private final OwnedCarService ownedCarService;
    private final CarModelService carModelService;
    private final PartService partService;
    private final UploadService uploadService;
    private final BlockService blockService;
    private final WishlistService wishlistService;

    // 차량 정보는 보유 차량에서 복사하고, 태그의 새 부품·브랜드는 이 자리에서 등록한다 (한 번의 요청으로 작성 완료)
    @Override
    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest request) {
        memberService.getById(memberId);
        OwnedCarSnapshot car = ownedCarService.getSnapshot(memberId, request.ownedCarId());
        Post post = Post.of(memberId, car, request.buildDirection(), normalizeContent(request.content()));
        post.replaceImages(buildImages(memberId, post, request.images()));
        postRepository.save(post);
        return toDetail(post, memberId);
    }

    @Override
    @Transactional
    public PostResponse update(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = getOwnPost(memberId, postId);
        if (request.ownedCarId() != null) {
            post.changeCar(ownedCarService.getSnapshot(memberId, request.ownedCarId()));
        }
        if (request.buildDirection() != null) {
            post.changeBuildDirection(request.buildDirection());
        }
        if (request.content() != null) {
            post.changeContent(normalizeContent(request.content()));
        }
        if (request.images() != null) {
            Set<Long> keptImageIds = request.images().stream()
                    .map(PostImageRequest::imageId)
                    .collect(Collectors.toSet());
            List<Long> removedImageIds = post.getImages().stream()
                    .map(PostImage::getUploadedImageId)
                    .filter(imageId -> !keptImageIds.contains(imageId))
                    .toList();
            post.replaceImages(buildImages(memberId, post, request.images()));
            uploadService.delete(removedImageIds);
        }
        return toDetail(post, memberId);
    }

    @Override
    @Transactional
    public PostResponse delete(Long memberId, Long postId) {
        Post post = getOwnPost(memberId, postId);
        List<Long> imageIds = post.getImages().stream()
                .map(PostImage::getUploadedImageId)
                .toList();
        postRepository.delete(post);
        uploadService.delete(imageIds);
        return PostResponse.message(PostStatusCode.DELETE_SUCCESS.getMessage());
    }

    // 숨김 처리된 게시물은 작성자 본인만 볼 수 있고, 차단 관계인 회원의 게시물은 없는 것처럼 응답한다
    @Override
    public PostResponse getPost(Long viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .filter(found -> found.isPublic() || found.isWrittenBy(viewerId))
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        if (viewerId != null && blockService.getHiddenMemberIds(viewerId).contains(post.getMemberId())) {
            throw new CustomException(PostStatusCode.POST_NOT_FOUND);
        }
        return toDetail(post, viewerId);
    }

    @Override
    public PostResponse getFeed(
            Long viewerId,
            List<Long> carModelIds,
            BuildDirection buildDirection,
            Long cursor,
            int size
    ) {
        Set<Long> hiddenAuthors = hiddenAuthorsFor(viewerId);
        Specification<Post> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PostStatus.PUBLIC),
                carModelIds.isEmpty() ? cb.conjunction() : root.get("carModelId").in(carModelIds),
                buildDirection != null ? cb.equal(root.get("buildDirection"), buildDirection) : cb.conjunction(),
                hiddenAuthors.isEmpty() ? cb.conjunction() : cb.not(root.get("memberId").in(hiddenAuthors)),
                cursor != null ? cb.lessThan(root.get("id"), cursor) : cb.conjunction()
        );
        return toCards(spec, size);
    }

    // 내 게시물 목록에는 숨김 처리된 게시물도 보여준다 (status 로 구분)
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
        return postRepository.findById(postId)
                .map(Post::getMemberId)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
    }

    @Override
    @Transactional
    public void hide(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND))
                .hide();
    }

    @EventListener
    @Transactional
    public void hideAll(MemberWithdrawnEvent event) {
        postRepository.hideAllByAuthor(event.memberId());
    }

    // 사진 파일과 업로드 기록은 업로드 도메인이 같은 이벤트로 지운다
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        postRepository.deleteAll(postRepository.findAllByMemberId(event.memberId()));
    }

    // 이미 붙어 있는 사진은 기존 PostImage 를 재사용하고, 새 사진만 업로드 기록에서 가져와 붙인다
    private List<PostImage> buildImages(Long memberId, Post post, List<PostImageRequest> requests) {
        List<Long> requestedIds = requests.stream()
                .map(PostImageRequest::imageId)
                .toList();
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new CustomException(PostStatusCode.DUPLICATE_IMAGE);
        }
        Map<Long, PostImage> existing = post.getImages().stream()
                .collect(Collectors.toMap(PostImage::getUploadedImageId, Function.identity()));
        Map<Long, ImageInfo> attached = uploadService.attach(
                memberId,
                requestedIds.stream().filter(imageId -> !existing.containsKey(imageId)).toList()
        ).stream().collect(Collectors.toMap(ImageInfo::id, Function.identity()));

        return IntStream.range(0, requests.size())
                .mapToObj(order -> {
                    PostImageRequest request = requests.get(order);
                    PostImage image = existing.containsKey(request.imageId())
                            ? existing.get(request.imageId())
                            : PostImage.of(post, attached.get(request.imageId()), order);
                    image.changeDisplayOrder(order);
                    image.clearTags();
                    request.tags().forEach(tag -> image.addTag(resolvePart(tag).id(), tag.x(), tag.y()));
                    return image;
                })
                .toList();
    }

    private PartInfo resolvePart(PartTagRequest tag) {
        return partService.resolve(new PartCommand(tag.partId(), tag.partName(), tag.brandName(), tag.category()));
    }

    private PostResponse toDetail(Post post, Long viewerId) {
        Member author = memberService.findActiveMember(post.getMemberId()).orElse(null);
        List<Long> partIds = post.getImages().stream()
                .flatMap(image -> image.getTags().stream())
                .map(PostPartTag::getPartId)
                .distinct()
                .toList();
        Set<Long> wishlisted = viewerId != null
                ? wishlistService.getWishlistedPartIds(viewerId, partIds)
                : Set.of();
        return PostResponse.detail(
                post,
                author,
                carModelService.getSpec(post.getCarModelId(), post.getCarGenerationId(), post.getCarTrimId()),
                partService.getPartInfos(partIds),
                wishlisted
        );
    }

    private PostResponse toCards(Specification<Post> spec, int size) {
        List<Post> found = postRepository.findBy(spec, query -> query
                .sortBy(Sort.by(Sort.Direction.DESC, "id"))
                .limit(size + 1)
                .all()
        );
        boolean hasNext = found.size() > size;
        List<Post> page = hasNext ? found.subList(0, size) : found;
        Map<Long, String> modelNames = carModelService.getModelNames(
                page.stream().map(Post::getCarModelId).collect(Collectors.toSet())
        );
        return PostResponse.cards(
                page.stream()
                        .map(post -> PostResponse.Card.of(post, modelNames.get(post.getCarModelId())))
                        .toList(),
                hasNext ? page.getLast().getId() : null
        );
    }

    private Post getOwnPost(Long memberId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        if (!post.isWrittenBy(memberId)) {
            throw new CustomException(PostStatusCode.NOT_POST_AUTHOR);
        }
        return post;
    }

    private Set<Long> hiddenAuthorsFor(Long viewerId) {
        return viewerId != null ? blockService.getHiddenMemberIds(viewerId) : Set.of();
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String stripped = content.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
