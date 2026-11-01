package ijiri.ijiriserver.domain.post.application.service;

import ijiri.ijiriserver.domain.member.application.port.in.GetMemberUseCase;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.ownedcar.application.port.in.GetOwnedCarSnapshotUseCase;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot;
import ijiri.ijiriserver.domain.part.application.data.PartCommand;
import ijiri.ijiriserver.domain.part.application.data.PartInfo;
import ijiri.ijiriserver.domain.part.application.port.in.RecordPartUsageUseCase;
import ijiri.ijiriserver.domain.part.application.port.in.ResolvePartUseCase;
import ijiri.ijiriserver.domain.post.application.data.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.application.data.request.PostImageRequest;
import ijiri.ijiriserver.domain.post.application.data.request.PostPartRequest;
import ijiri.ijiriserver.domain.post.application.data.request.PostTagRequest;
import ijiri.ijiriserver.domain.post.application.data.request.PostUpdateRequest;
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse;
import ijiri.ijiriserver.domain.post.application.port.in.CreatePostUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.DeletePostUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.HidePostUseCase;
import ijiri.ijiriserver.domain.post.application.port.in.UpdatePostUseCase;
import ijiri.ijiriserver.domain.post.application.port.out.PostPort;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostImage;
import ijiri.ijiriserver.domain.post.entity.PostPart;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import ijiri.ijiriserver.domain.post.event.PostDeletedEvent;
import ijiri.ijiriserver.domain.post.event.PostPartsRemovedEvent;
import ijiri.ijiriserver.domain.post.exception.PostStatusCode;
import ijiri.ijiriserver.domain.upload.application.data.AttachedImage;
import ijiri.ijiriserver.domain.upload.application.port.in.AttachUploadedImagesUseCase;
import ijiri.ijiriserver.domain.upload.application.port.in.DeleteUploadedImagesUseCase;
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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
public class PostCommandService implements CreatePostUseCase,
        UpdatePostUseCase,
        DeletePostUseCase,
        HidePostUseCase {

    private final PostPort postPort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetOwnedCarSnapshotUseCase getOwnedCarSnapshotUseCase;
    private final RecordPartUsageUseCase recordPartUsageUseCase;
    private final ResolvePartUseCase resolvePartUseCase;
    private final AttachUploadedImagesUseCase attachUploadedImagesUseCase;
    private final DeleteUploadedImagesUseCase deleteUploadedImagesUseCase;
    private final ApplicationEventPublisher eventPublisher;

    // 사진은 업로드 기록에서 이 회원이 올린 것인지 확인해 붙이고, 새 부품·브랜드는 이 자리에서 등록한다 (한 번의 요청으로 작성 완료)
    @Override
    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest request) {
        getMemberUseCase.getById(memberId);
        OwnedCarSnapshot car = getOwnedCarSnapshotUseCase.getSnapshot(memberId, request.ownedCarId());
        if (request.images().stream().anyMatch(image -> image.width() == null || image.height() == null)) {
            throw new CustomException(PostStatusCode.MISSING_IMAGE_SIZE);
        }
        Post post = Post.of(memberId, car, request.buildStyle(), normalizeContent(request.content()));
        List<AttachedImage> attached = attachUploadedImagesUseCase.attach(
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
        postPort.save(post);
        recordPartUsageUseCase.recordUsage(distinctPartIds(post.getParts()), post.getCarModelId(), 1);
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
        recordPartUsageUseCase.recordUsage(difference(afterPartIds, beforePartIds), post.getCarModelId(), 1);
        recordPartUsageUseCase.recordUsage(difference(beforePartIds, afterPartIds), post.getCarModelId(), -1);
        if (!removed.isEmpty()) {
            eventPublisher.publishEvent(new PostPartsRemovedEvent(removed));
        }
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long postId) {
        deletePost(getOwnPost(memberId, postId));
    }

    @Override
    @Transactional
    public void hide(Long postId) {
        postPort.findById(postId).ifPresent(Post::hide);
    }

    @EventListener
    @Transactional
    public void hideAll(MemberWithdrawnEvent event) {
        postPort.hideAllByAuthor(event.memberId());
    }

    // 영구 삭제 순서: 위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원. 사진 파일은 업로드 도메인이 같은 이벤트로 지운다
    @Order(1)
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        postPort.findAllByMemberId(event.memberId()).forEach(this::deletePost);
    }

    private void deletePost(Post post) {
        List<String> imageKeys = post.getImages().stream()
                .map(PostImage::getImageKey)
                .toList();
        eventPublisher.publishEvent(new PostDeletedEvent(post.getId()));
        recordPartUsageUseCase.recordUsage(distinctPartIds(post.getParts()), post.getCarModelId(), -1);
        // 태그는 사진을 FK 로 가리키는데 Hibernate 의 삭제 순서는 보장되지 않으므로, 부품·태그를 먼저 지운다
        post.replaceParts(List.of());
        postPort.flush();
        postPort.delete(post);
        deleteUploadedImagesUseCase.delete(imageKeys);
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
        return resolvePartUseCase.resolve(new PartCommand(
                request.partId(),
                request.brandName(),
                request.partName(),
                request.category()
        ));
    }

    private Post getOwnPost(Long memberId, Long postId) {
        Post post = postPort.findById(postId)
                .filter(found -> found.getStatus() != PostStatus.AUTHOR_WITHDRAWN)
                .orElseThrow(() -> new CustomException(PostStatusCode.POST_NOT_FOUND));
        if (!post.isWrittenBy(memberId)) {
            throw new CustomException(PostStatusCode.NOT_POST_AUTHOR);
        }
        return post;
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
