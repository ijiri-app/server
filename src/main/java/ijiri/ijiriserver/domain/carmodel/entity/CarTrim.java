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
 * 차종 마스터의 3단계(트림).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "car_trim",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_car_trim_car_generation_id_name",
                columnNames = {"car_generation_id", "name"}
        )
)
public class CarTrim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_generation_id", nullable = false, updatable = false)
    private Long carGenerationId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
