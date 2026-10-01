package ijiri.ijiriserver.domain.post.entity;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.ownedcar.dto.OwnedCarSnapshot;
import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시물. 보유 차량을 가리키고, 피드 필터·카드용으로 차종·트림·연식을 복사해 둔다 (보유 차량의 트림은 바뀌지 않는다).
 * partCount 는 피드 카드에 보여줄 부품 수로, 부품을 바꿀 때마다 다시 센다.
 * 삭제할 때 태그(부품 쪽)가 사진보다 먼저 지워지도록 parts 를 images 앞에 둔다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "post",
        indexes = {
                @Index(name = "idx_post_member_id", columnList = "member_id"),
                @Index(name = "idx_post_owned_car_id", columnList = "owned_car_id"),
                @Index(name = "idx_post_status_car_model_id", columnList = "status, car_model_id")
        }
)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "owned_car_id", nullable = false, updatable = false)
    private Long ownedCarId;

    @Column(name = "car_model_id", nullable = false, updatable = false)
    private Long carModelId;

    @Column(name = "car_generation_id", nullable = false, updatable = false)
    private Long carGenerationId;

    @Column(name = "car_trim_id", nullable = false, updatable = false)
    private Long carTrimId;

    @Column(name = "model_year", nullable = false, updatable = false)
    private int modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_style", nullable = false, length = 20)
    private BuildStyle buildStyle;

    @Column(name = "content", length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "part_count", nullable = false)
    private int partCount;

    @Builder.Default
    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostPart> parts = new ArrayList<>();

    @Builder.Default
    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();

    public static Post of(Long memberId, OwnedCarSnapshot car, BuildStyle buildStyle, String content) {
        return Post.builder()
                .memberId(memberId)
                .ownedCarId(car.ownedCarId())
                .carModelId(car.spec().carModelId())
                .carGenerationId(car.spec().generationId())
                .carTrimId(car.spec().trimId())
                .modelYear(car.modelYear())
                .buildStyle(buildStyle)
                .content(content)
                .status(PostStatus.PUBLIC)
                .build();
    }

    public void changeBuildStyle(BuildStyle buildStyle) {
        this.buildStyle = buildStyle;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void addImage(PostImage image) {
        images.add(image);
    }

    // 유지되는 부품은 같은 PostPart 를 다시 넣어 postPartId(위시리스트가 가리키는 id)를 지킨다
    public void replaceParts(List<PostPart> newParts) {
        parts.clear();
        parts.addAll(newParts);
        partCount = newParts.size();
    }

    public void hide() {
        if (status == PostStatus.PUBLIC) {
            status = PostStatus.HIDDEN;
        }
    }

    public boolean isPublic() {
        return status == PostStatus.PUBLIC;
    }

    public boolean isWrittenBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public PostImage getThumbnail() {
        return images.isEmpty() ? null : images.getFirst();
    }
}
