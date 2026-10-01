package ijiri.ijiriserver.domain.carmodel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 차종 마스터의 1단계(모델). 관심 차종과 피드 탭이 이 단위로 묶인다. 예) 아반떼 N, 아반떼 N 라인
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "car_model",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_car_model_manufacturer_name",
                columnNames = {"manufacturer", "name"}
        )
)
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manufacturer", nullable = false, length = 50)
    private String manufacturer;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // 목록에 보이는 순서 (CSV 에 처음 나온 순서)
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
