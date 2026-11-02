package ijiri.ijiriserver.domain.part.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 차종별로 이 부품이 달린 게시물 수. 부품 검색에서 같은 차종에 자주 달린 부품을 위로 올린다.
 */
@Entity
@Table(
    name = "part_car_model_usage",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_part_car_model_usage_part_id_car_model_id",
            columnNames = ["part_id", "car_model_id"],
        ),
    ],
)
class PartCarModelUsage(
    partId: Long,
    carModelId: Long,
    useCount: Int = 0,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "part_id", nullable = false)
    var partId: Long = partId
        protected set

    @Column(name = "car_model_id", nullable = false)
    var carModelId: Long = carModelId
        protected set

    @Column(name = "use_count", nullable = false)
    var useCount: Int = useCount
        protected set
}
