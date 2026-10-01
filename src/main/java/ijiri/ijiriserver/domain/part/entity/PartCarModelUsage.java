package ijiri.ijiriserver.domain.part.entity;

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
 * 차종별로 이 부품이 달린 게시물 수. 부품 검색에서 같은 차종에 자주 달린 부품을 위로 올린다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "part_car_model_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_part_car_model_usage_part_id_car_model_id",
                columnNames = {"part_id", "car_model_id"}
        )
)
public class PartCarModelUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_id", nullable = false)
    private Long partId;

    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;

    @Column(name = "use_count", nullable = false)
    private int useCount;
}
