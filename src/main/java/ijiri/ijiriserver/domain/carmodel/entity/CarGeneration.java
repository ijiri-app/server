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
 * 차종 마스터의 2단계(세대). 예) CN7, CN7 PE
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "car_generation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_car_generation_car_model_id_code",
                columnNames = {"car_model_id", "code"}
        )
)
public class CarGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_model_id", nullable = false, updatable = false)
    private Long carModelId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_year", nullable = false)
    private int startYear;

    // null 이면 현재 판매 중
    @Column(name = "end_year")
    private Integer endYear;
}
