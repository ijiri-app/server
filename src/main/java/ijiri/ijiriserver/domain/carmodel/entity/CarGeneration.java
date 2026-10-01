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

    @Column(name = "start_year", nullable = false)
    private int startYear;

    // null 이면 현재 판매 중
    @Column(name = "end_year")
    private Integer endYear;

    // 보유 차량 연식은 세대의 판매 기간 안에서만 고를 수 있다 (판매 중이면 올해 + 1 까지)
    public boolean coversYear(int year, int currentYear) {
        int lastYear = endYear != null ? endYear : currentYear + 1;
        return year >= startYear && year <= lastYear;
    }
}
