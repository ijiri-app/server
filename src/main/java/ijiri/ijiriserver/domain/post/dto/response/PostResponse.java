package ijiri.ijiriserver.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostImage;
import ijiri.ijiriserver.domain.post.entity.PostPartTag;
import ijiri.ijiriserver.domain.post.entity.PostStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * post 도메인의 모든 API 응답.
 * 상세(작성/수정 포함)는 게시물 필드, 목록(피드, 회원 게시물)은 posts + nextCursor, 삭제는 message 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostResponse(
        Long id,
        Author author,
        Car car,
        BuildDirection buildDirection,
        String content,
        List<Image> images,
        List<PartGroup> partGroups,
        Integer partCount,
        PostStatus status,
        LocalDateTime createdAt,
        List<Card> posts,
        Long nextCursor,
        String message
) {

    public static PostResponse detail(
            Post post,
            Member author,
            CarSpec spec,
            Map<Long, PartInfo> parts,
            Set<Long> wishlistedPartIds
    ) {
        return new PostResponse(
                post.getId(),
                author != null ? new Author(author.getId(), author.getNickname(), author.getProfileImageUrl()) : null,
                Car.of(spec, post.getModelYear()),
                post.getBuildDirection(),
                post.getContent(),
                post.getImages().stream()
                        .map(image -> Image.of(image, parts))
                        .toList(),
                partGroups(post, parts, wishlistedPartIds),
                post.getPartCount(),
                post.getStatus(),
                post.getCreatedAt(),
                null,
                null,
                null
        );
    }

    public static PostResponse cards(List<Card> posts, Long nextCursor) {
        return new PostResponse(
                null, null, null, null, null, null, null, null, null, null,
                posts,
                nextCursor,
                null
        );
    }

    public static PostResponse message(String message) {
        return new PostResponse(
                null, null, null, null, null, null, null, null, null, null, null, null,
                message
        );
    }

    // 분류 순서(익스테리어, 인테리어, 파워트레인, 하체)대로, 분류 안에서는 사진·태그에 처음 나온 순서대로
    private static List<PartGroup> partGroups(Post post, Map<Long, PartInfo> parts, Set<Long> wishlistedPartIds) {
        List<PartInfo> tagged = post.getImages().stream()
                .flatMap(image -> image.getTags().stream())
                .map(PostPartTag::getPartId)
                .distinct()
                .map(parts::get)
                .filter(Objects::nonNull)
                .toList();
        return Arrays.stream(PartCategory.values())
                .map(category -> new PartGroup(
                        category,
                        tagged.stream()
                                .filter(part -> part.category() == category)
                                .map(part -> new PartItem(
                                        part.id(),
                                        part.name(),
                                        part.brandName(),
                                        wishlistedPartIds.contains(part.id())
                                ))
                                .toList()
                ))
                .filter(group -> !group.parts().isEmpty())
                .toList();
    }

    public record Author(
            Long id,
            String nickname,
            String profileImageUrl
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Car(
            Long carModelId,
            String manufacturer,
            String modelName,
            Long carGenerationId,
            String generationCode,
            Long carTrimId,
            String trimName,
            Integer modelYear
    ) {

        static Car of(CarSpec spec, Integer modelYear) {
            return new Car(
                    spec.carModelId(),
                    spec.manufacturer(),
                    spec.modelName(),
                    spec.generationId(),
                    spec.generationCode(),
                    spec.trimId(),
                    spec.trimName(),
                    modelYear
            );
        }
    }

    public record Image(
            Long imageId,
            String url,
            Integer width,
            Integer height,
            List<Tag> tags
    ) {

        static Image of(PostImage image, Map<Long, PartInfo> parts) {
            return new Image(
                    image.getUploadedImageId(),
                    image.getUrl(),
                    image.getWidth(),
                    image.getHeight(),
                    image.getTags().stream()
                            .filter(tag -> parts.containsKey(tag.getPartId()))
                            .map(tag -> Tag.of(tag, parts.get(tag.getPartId())))
                            .toList()
            );
        }
    }

    public record Tag(
            Long partId,
            String partName,
            String brandName,
            PartCategory category,
            double x,
            double y
    ) {

        static Tag of(PostPartTag tag, PartInfo part) {
            return new Tag(part.id(), part.name(), part.brandName(), part.category(), tag.getX(), tag.getY());
        }
    }

    public record PartGroup(
            PartCategory category,
            List<PartItem> parts
    ) {
    }

    public record PartItem(
            Long partId,
            String name,
            String brandName,
            boolean wishlisted
    ) {
    }

    /**
     * 피드 2열 벽돌형 카드. 썸네일 크기를 모르면(HEIC 등) width, height 가 null 이다.
     */
    public record Card(
            Long id,
            String thumbnailUrl,
            Integer thumbnailWidth,
            Integer thumbnailHeight,
            Long carModelId,
            String carModelName,
            Integer modelYear,
            BuildDirection buildDirection,
            int partCount,
            PostStatus status
    ) {

        public static Card of(Post post, String carModelName) {
            PostImage thumbnail = post.getThumbnail();
            return new Card(
                    post.getId(),
                    thumbnail != null ? thumbnail.getUrl() : null,
                    thumbnail != null ? thumbnail.getWidth() : null,
                    thumbnail != null ? thumbnail.getHeight() : null,
                    post.getCarModelId(),
                    carModelName,
                    post.getModelYear(),
                    post.getBuildDirection(),
                    post.getPartCount(),
                    post.getStatus()
            );
        }
    }
}
