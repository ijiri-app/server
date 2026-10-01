package ijiri.ijiriserver.domain.ownedcar.entity;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
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
 * 회원의 보유 차량. 게시물을 올릴 때 이 중 하나를 고른다.
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

    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;

    @Column(name = "car_generation_id", nullable = false)
    private Long carGenerationId;

    @Column(name = "car_trim_id")
    private Long carTrimId;

    @Column(name = "model_year")
    private Integer modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_direction", length = 20)
    private BuildDirection buildDirection;

    public static OwnedCar of(Long memberId, CarSpec spec, Integer modelYear, BuildDirection buildDirection) {
        return OwnedCar.builder()
                .memberId(memberId)
                .carModelId(spec.carModelId())
                .carGenerationId(spec.generationId())
                .carTrimId(spec.trimId())
                .modelYear(modelYear)
                .buildDirection(buildDirection)
                .build();
    }

    public void update(CarSpec spec, Integer modelYear, BuildDirection buildDirection) {
        this.carModelId = spec.carModelId();
        this.carGenerationId = spec.generationId();
        this.carTrimId = spec.trimId();
        this.modelYear = modelYear;
        this.buildDirection = buildDirection;
    }
}
