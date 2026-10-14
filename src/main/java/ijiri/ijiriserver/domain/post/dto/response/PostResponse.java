package ijiri.ijiriserver.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostImage;
import ijiri.ijiriserver.domain.post.entity.PostPart;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * post 도메인의 모든 API 응답. 상세는 게시물 필드, 목록(피드, 회원 게시물)은 items + nextCursor,
 * 작성은 id 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostResponse(
        Long id,
        Author author,
        Car car,
        String content,
        List<Image> images,
        List<Part> parts,
        Long wishCount,
        Boolean isMine,
        LocalDateTime createdAt,
        List<Card> items,
        String nextCursor
) {

    public static PostResponse created(Long id) {
        return new PostResponse(id, null, null, null, null, null, null, null, null, null, null);
    }

    public static PostResponse cards(List<Card> items, Long nextCursor) {
        return new PostResponse(
                null, null, null, null, null, null, null, null, null,
                items,
                nextCursor != null ? String.valueOf(nextCursor) : null
        );
    }

    public static PostResponse detail(
            Post post,
            Member author,
            CarSpec spec,
            Map<Long, PartInfo> partInfos,
            Map<Long, Long> wishCounts,
            Set<Long> wished,
            boolean isMine
    ) {
        List<Part> parts = post.getParts().stream()
                .sorted(Comparator.comparing(PostPart::getCategory).thenComparing(PostPart::getDisplayOrder))
                .map(part -> Part.of(
                        part,
                        partInfos.get(part.getPartId()),
                        wishCounts.getOrDefault(part.getId(), 0L),
                        wished.contains(part.getId())
                ))
                .toList();
        return new PostResponse(
                post.getId(),
                author != null ? Author.from(author) : null,
                Car.of(post, spec),
                post.getContent(),
                post.getImages().stream()
                        .map(image -> Image.of(image, post.getParts()))
                        .toList(),
                parts,
                parts.stream().mapToLong(Part::wishCount).sum(),
                isMine,
                post.getCreatedAt(),
                null,
                null
        );
    }

    public record Author(
            Long id,
            String nickname,
            String profileImageUrl,
            String statusMessage
    ) {

        static Author from(Member member) {
            return new Author(
                    member.getId(),
                    member.getNickname(),
                    member.getProfileImageUrl(),
                    member.getStatusMessage()
            );
        }
    }

    public record Car(
            Long ownedCarId,
            Long carModelId,
            String carModelName,
            String generationCode,
            String trimName,
            int year,
            BuildStyle buildStyle
    ) {

        static Car of(Post post, CarSpec spec) {
            return new Car(
                    post.getOwnedCarId(),
                    spec.carModelId(),
                    spec.modelName(),
                    spec.generationCode(),
                    spec.trimName(),
                    post.getModelYear(),
                    post.getBuildStyle()
            );
        }
    }

    // imageKey 는 게시물 수정에서 태그를 이 사진에 다시 걸 때 쓴다
    public record Image(
            Long id,
            String imageKey,
            String url,
            int width,
            int height,
            List<Tag> tags
    ) {

        static Image of(PostImage image, List<PostPart> parts) {
            return new Image(
                    image.getId(),
                    image.getImageKey(),
                    image.getUrl(),
                    image.getWidth(),
                    image.getHeight(),
                    parts.stream()
                            .flatMap(part -> part.getTags().stream())
                            .filter(tag -> tag.getPostImage().getId().equals(image.getId()))
                            .map(tag -> new Tag(tag.getPostPart().getId(), tag.getX(), tag.getY()))
                            .toList()
            );
        }
    }

    public record Tag(
            Long postPartId,
            double x,
            double y
    ) {
    }

    public record Part(
            Long postPartId,
            Long partId,
            PartCategory category,
            String brandName,
            String partName,
            long wishCount,
            boolean wished
    ) {

        static Part of(PostPart part, PartInfo info, long wishCount, boolean wished) {
            return new Part(
                    part.getId(),
                    part.getPartId(),
                    part.getCategory(),
                    info != null ? info.brandName() : null,
                    info != null ? info.name() : null,
                    wishCount,
                    wished
            );
        }
    }

    /**
     * 피드 2열 벽돌형 카드. 썸네일 크기로 카드 높이를 정한다. hidden 은 내 게시물 목록에서 숨김 처리된 게시물만 true.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Card(
            Long postId,
            String thumbnailUrl,
            Integer thumbWidth,
            Integer thumbHeight,
            String carModelName,
            int year,
            int partCount,
            Boolean hidden
    ) {

        public static Card of(Post post, String carModelName) {
            PostImage thumbnail = post.getThumbnail();
            return new Card(
                    post.getId(),
                    thumbnail != null ? thumbnail.getUrl() : null,
                    thumbnail != null ? thumbnail.getWidth() : null,
                    thumbnail != null ? thumbnail.getHeight() : null,
                    carModelName,
                    post.getModelYear(),
                    post.getPartCount(),
                    post.isPublic() ? null : true
            );
        }
    }
}
