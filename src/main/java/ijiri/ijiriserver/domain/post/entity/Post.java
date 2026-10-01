package ijiri.ijiriserver.domain.post.entity;

import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
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
 * 게시물. 차량 정보는 작성 시점의 보유 차량에서 복사해 두어, 보유 차량을 고치거나 지워도 바뀌지 않는다.
 * partCount 는 피드 카드에 보여줄 태그된 부품 수(중복 제외)로, 사진을 바꿀 때마다 다시 센다.
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
                @Index(name = "idx_post_status_car_model_id", columnList = "status, car_model_id")
        }
)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;

    @Column(name = "car_generation_id", nullable = false)
    private Long carGenerationId;

    @Column(name = "car_trim_id")
    private Long carTrimId;

    @Column(name = "model_year")
    private Integer modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_direction", nullable = false, length = 20)
    private BuildDirection buildDirection;

    @Column(name = "content", length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "part_count", nullable = false)
    private int partCount;

    @Builder.Default
    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();

    public static Post of(Long memberId, OwnedCarSnapshot car, BuildDirection buildDirection, String content) {
        return Post.builder()
                .memberId(memberId)
                .carModelId(car.spec().carModelId())
                .carGenerationId(car.spec().generationId())
                .carTrimId(car.spec().trimId())
                .modelYear(car.modelYear())
                .buildDirection(buildDirection)
                .content(content)
                .status(PostStatus.PUBLIC)
                .build();
    }

    public void changeCar(OwnedCarSnapshot car) {
        this.carModelId = car.spec().carModelId();
        this.carGenerationId = car.spec().generationId();
        this.carTrimId = car.spec().trimId();
        this.modelYear = car.modelYear();
    }

    public void changeBuildDirection(BuildDirection buildDirection) {
        this.buildDirection = buildDirection;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    // 유지되는 사진은 같은 PostImage 를 다시 넣어야 한다. 새 엔티티로 바꾸면 삭제보다 INSERT 가 먼저 실행돼
    // uploaded_image_id 유니크 제약에 걸린다
    public void replaceImages(List<PostImage> newImages) {
        images.clear();
        images.addAll(newImages);
        partCount = (int) newImages.stream()
                .flatMap(image -> image.getTags().stream())
                .map(PostPartTag::getPartId)
                .distinct()
                .count();
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
