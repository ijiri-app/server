package ijiri.ijiriserver.domain.ownedcar.entity;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 보유 차량. 트림을 고르면 모델·세대가 정해지고, 게시물을 올릴 때 이 중 하나를 고른다.
 * 트림은 바꿀 수 없다 (게시물이 이 차량을 가리키므로).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "owned_car",
        indexes = @Index(name = "idx_owned_car_member_id", columnList = "member_id")
)
public class OwnedCar extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "car_model_id", nullable = false, updatable = false)
    private Long carModelId;

    @Column(name = "car_generation_id", nullable = false, updatable = false)
    private Long carGenerationId;

    @Column(name = "car_trim_id", nullable = false, updatable = false)
    private Long carTrimId;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_style", length = 20)
    private BuildStyle buildStyle;

    @Column(name = "nickname", length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OwnedCarStatus status;

    public static OwnedCar of(Long memberId, CarSpec spec, int modelYear, BuildStyle buildStyle, String nickname) {
        return OwnedCar.builder()
                .memberId(memberId)
                .carModelId(spec.carModelId())
                .carGenerationId(spec.generationId())
                .carTrimId(spec.trimId())
                .modelYear(modelYear)
                .buildStyle(buildStyle)
                .nickname(nickname)
                .status(OwnedCarStatus.OWNED)
                .build();
    }

    public void changeModelYear(int modelYear) {
        this.modelYear = modelYear;
    }

    public void changeBuildStyle(BuildStyle buildStyle) {
        this.buildStyle = buildStyle;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeStatus(OwnedCarStatus status) {
        this.status = status;
    }
}
