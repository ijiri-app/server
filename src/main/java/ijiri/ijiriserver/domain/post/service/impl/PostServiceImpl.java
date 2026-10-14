package ijiri.ijiriserver.domain.post.service.impl;

import ijiri.ijiriserver.domain.block.service.BlockService;
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.carmodel.service.CarModelService;
import ijiri.ijiriserver.domain.interestcar.service.InterestCarService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.ownedcar.dto.OwnedCarSnapshot;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarService;
import ijiri.ijiriserver.domain.part.dto.PartCommand;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.service.PartService;
import ijiri.ijiriserver.domain.post.dto.PostPartSummary;
import ijiri.ijiriserver.domain.post.dto.WishTarget;
import ijiri.ijiriserver.domain.post.dto.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostImageRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostPartRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostTagRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostUpdateRequest;
import ijiri.ijiriserver.domain.post.dto.response.PostResponse;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostImage;
import ijiri.ijiriserver.domain.post.entity.PostPart;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import ijiri.ijiriserver.domain.post.event.PostDeletedEvent;
import ijiri.ijiriserver.domain.post.event.PostPartsRemovedEvent;
import ijiri.ijiriserver.domain.post.exception.PostStatusCode;
import ijiri.ijiriserver.domain.post.repository.PostPartRepository;
import ijiri.ijiriserver.domain.post.repository.PostRepository;
import ijiri.ijiriserver.domain.post.service.PostService;
import ijiri.ijiriserver.domain.post.service.PostWishReader;
import ijiri.ijiriserver.domain.upload.dto.AttachedImage;
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;
import ijiri.ijiriserver.domain.upload.service.UploadService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
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
    private final PostPartRepository postPartRepository;
    private final MemberService memberService;
    private final OwnedCarService ownedCarService;
    private final CarModelService carModelService;
    private final InterestCarService interestCarService;
    private final PartService partService;
    private final UploadService uploadService;
    private final BlockService blockService;
    private final PostWishReader postWishReader;
    private final ApplicationEventPublisher eventPublisher;

    // 사진은 업로드 기록에서 이 회원이 올린 것인지 확인해 붙이고, 새 부품·브랜드는 이 자리에서 등록한다 (한 번의 요청으로 작성 완료)
    @Override
    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest request) {
        memberService.getById(memberId);
        OwnedCarSnapshot car = ownedCarService.getSnapshot(memberId, request.ownedCarId());
        if (request.images().stream().anyMatch(image -> image.width() == null || image.height() == null)) {
            throw new CustomException(PostStatusCode.MISSING_IMAGE_SIZE);
        }
        Post post = Post.of(memberId, car, request.buildStyle(), normalizeContent(request.content()));
        List<AttachedImage> attached = uploadService.attach(
                memberId,
                request.images().stream().map(PostImageRequest::imageKey).toList(),
                UploadPurpose.POST
        );
        // 태그는 업로드 때 받은 키로 사진을 가리키지만, 연결하면서 사진은 영구 키로 옮겨진다
        Map<String, PostImage> imagesByRequestKey = new HashMap<>();
        IntStream.range(0, attached.size()).forEach(order -> {
            PostImageRequest image = request.images().get(order);
            PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageKey(attached.get(order).imageKey())
                    .url(attached.get(order).url())
                    .width(image.width())
                    .height(image.height())
                    .displayOrder(order)
                    .build();
            post.addImage(postImage);
            imagesByRequestKey.put(image.imageKey(), postImage);
        });
        post.replaceParts(buildParts(post, request.parts(), request.images(), imagesByRequestKey, Map.of()));
        postRepository.save(post);
        partService.recordUsage(distinctPartIds(post.getParts()), post.getCarModelId(), 1);
        return PostResponse.created(post.getId());
    }

    // 부품을 바꾸면 빠진 부품의 담기를 지우고, 부품별 사용 수를 다시 맞춘다
    @Override
    @Transactional
    public void update(Long memberId, Long postId, PostUpdateRequest request) {
        // 사진은 바꿀 수 없고 images 는 parts 의 태그를 싣는 용도라, parts 없이 오면 조용히 무시하지 않고 거절한다
        if (request.parts() == null && request.images() != null) {
            throw new CustomException(PostStatusCode.TAGS_WITHOUT_PARTS);
        }
        Post post = getOwnPost(memberId, postId);
        if (request.buildStyle() != null) {
            post.changeBuildStyle(request.buildStyle());
        }
        if (request.content() != null) {
            post.changeContent(normalizeContent(request.content()));
        }
        if (request.parts() == null) {
            return;
        }
        Map<Long, PostPart> existing = post.getParts().stream()
                .collect(Collectors.toMap(PostPart::getId, Function.identity()));
        Set<Long> beforePartIds = distinctPartIds(post.getParts());
        List<PostPart> parts = buildParts(
                post,
                request.parts(),
                request.images() != null ? request.images() : List.of(),
                post.getImages().stream().collect(Collectors.toMap(PostImage::getImageKey, Function.identity())),
                existing
        );
        List<Long> removed = existing.keySet().stream()
                .filter(id -> parts.stream().noneMatch(part -> id.equals(part.getId())))
                .toList();
        post.replaceParts(parts);

        Set<Long> afterPartIds = distinctPartIds(parts);
        partService.recordUsage(difference(afterPartIds, beforePartIds), post.getCarModelId(), 1);
        partService.recordUsage(difference(beforePartIds, afterPartIds), post.getCarModelId(), -1);
        if (!removed.isEmpty()) {
            eventPublisher.publishEvent(new PostPartsRemovedEvent(removed));
        }
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long postId) {
        deletePost(getOwnPost(memberId, postId));
    }

    // 숨김 처리된 게시물은 작성자 본인만 볼 수 있고, 차단 관계인 회원의 게시물은 없는 것처럼 응답한다
    @Override
    public PostResponse getPost(Long viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .filter(found -> found.isPublic()
                        || (found.isWrittenBy(viewerId) && found.getStatus() == PostStatus.HIDDEN))
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        if (viewerId != null && blockService.getHiddenMemberIds(viewerId).contains(post.getMemberId())) {
            throw new CustomException(PostStatusCode.POST_NOT_FOUND);
        }
        Member author = memberService.findActiveMember(post.getMemberId()).orElse(null);
        List<Long> postPartIds = post.getParts().stream().map(PostPart::getId).toList();
        return PostResponse.detail(
                post,
                author,
                carModelService.getSpecByTrim(post.getCarTrimId()),
                partService.getPartInfos(distinctPartIds(post.getParts())),
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
        return postRepository.findById(postId)
                .map(Post::getMemberId)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
    }

    @Override
    @Transactional
    public void hide(Long postId) {
        postRepository.findById(postId).ifPresent(Post::hide);
    }

    @Override
    public WishTarget getWishTarget(Long postPartId) {
        PostPart part = postPartRepository.findById(postPartId)
                .filter(found -> found.getPost().isPublic())
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        return new WishTarget(part.getId(), part.getPost().getId(), part.getPost().getMemberId());
    }

    @Override
    public Map<Long, PostPartSummary> getPostPartSummaries(Collection<Long> postPartIds) {
        List<PostPart> parts = postPartRepository.findAllById(postPartIds).stream()
                .filter(part -> part.getPost().isPublic())
                .toList();
        Map<Long, PartInfo> infos = partService.getPartInfos(distinctPartIds(parts));
        Map<Long, String> modelNames = carModelService.getModelNames(
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

    @EventListener
    @Transactional
    public void hideAll(MemberWithdrawnEvent event) {
        postRepository.hideAllByAuthor(event.memberId());
    }

    // 영구 삭제 순서: 위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원. 사진 파일은 업로드 도메인이 같은 이벤트로 지운다
    @Order(1)
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        postRepository.findAllByMemberId(event.memberId()).forEach(this::deletePost);
    }

    private void deletePost(Post post) {
        List<String> imageKeys = post.getImages().stream()
                .map(PostImage::getImageKey)
                .toList();
        eventPublisher.publishEvent(new PostDeletedEvent(post.getId()));
        partService.recordUsage(distinctPartIds(post.getParts()), post.getCarModelId(), -1);
        // 태그는 사진을 FK 로 가리키는데 Hibernate 의 삭제 순서는 보장되지 않으므로, 부품·태그를 먼저 지운다
        post.replaceParts(List.of());
        postRepository.flush();
        postRepository.delete(post);
        uploadService.delete(imageKeys);
    }

    // ref 로 태그와 부품을 잇는다. 수정할 때 postPartId 가 있는 부품은 기존 PostPart 를 그대로 쓴다
    private List<PostPart> buildParts(
            Post post,
            List<PostPartRequest> partRequests,
            List<PostImageRequest> imageRequests,
            Map<String, PostImage> images,
            Map<Long, PostPart> existing
    ) {
        Map<String, PostPart> byRef = new HashMap<>();
        List<PostPart> parts = IntStream.range(0, partRequests.size())
                .mapToObj(order -> {
                    PostPartRequest request = partRequests.get(order);
                    PostPart part = request.postPartId() != null && existing.containsKey(request.postPartId())
                            ? keepExisting(existing.get(request.postPartId()), request, byRef.values())
                            : PostPart.of(post, resolvePart(request).id(), request.category(), order);
                    part.reorder(request.category(), order);
                    part.clearTags();
                    if (byRef.put(request.ref(), part) != null) {
                        throw new CustomException(PostStatusCode.INVALID_PART_REF);
                    }
                    return part;
                })
                .toList();

        for (PostImageRequest imageRequest : imageRequests) {
            PostImage image = images.get(imageRequest.imageKey());
            for (PostTagRequest tag : imageRequest.tags()) {
                PostPart part = byRef.get(tag.ref());
                if (image == null || part == null) {
                    throw new CustomException(PostStatusCode.INVALID_PART_REF);
                }
                part.addTag(image, tag.x(), tag.y());
            }
        }
        return parts;
    }

    // 유지하는 부품(postPartId)은 담기 기록이 가리키는 대상이라 다른 부품으로 바꿀 수 없다.
    // 바꾸려면 postPartId 없이 새 부품으로 보내고, 같은 postPartId 를 두 번 보내면 거절한다
    private PostPart keepExisting(PostPart part, PostPartRequest request, Collection<PostPart> alreadyUsed) {
        boolean changesPart = request.partId() != null && !request.partId().equals(part.getPartId());
        if (changesPart || alreadyUsed.contains(part)) {
            throw new CustomException(PostStatusCode.INVALID_PART_REF);
        }
        return part;
    }

    private PartInfo resolvePart(PostPartRequest request) {
        return partService.resolve(new PartCommand(
                request.partId(),
                request.brandName(),
                request.partName(),
                request.category()
        ));
    }

    private List<Long> resolveFeedCarModels(Long viewerId, Long carModelId, List<Long> carModelIds) {
        if (carModelId != null) {
            return List.of(carModelId);
        }
        if (!carModelIds.isEmpty()) {
            return carModelIds;
        }
        return viewerId != null ? interestCarService.getCarModelIds(viewerId) : List.of();
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
                .filter(found -> found.getStatus() != PostStatus.AUTHOR_WITHDRAWN)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        if (!post.isWrittenBy(memberId)) {
            throw new CustomException(PostStatusCode.NOT_POST_AUTHOR);
        }
        return post;
    }

    private Set<Long> hiddenAuthorsFor(Long viewerId) {
        return viewerId != null ? blockService.getHiddenMemberIds(viewerId) : Set.of();
    }

    private Set<Long> distinctPartIds(List<PostPart> parts) {
        return parts.stream()
                .map(PostPart::getPartId)
                .collect(Collectors.toSet());
    }

    private Set<Long> difference(Set<Long> left, Set<Long> right) {
        Set<Long> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String stripped = content.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
