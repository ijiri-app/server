package ijiri.ijiriserver.domain.carmodel.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 차종 마스터의 2단계(세대). 예) CN7, CN7 PE
 */
@Entity
@Table(
    name = "car_generation",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_car_generation_car_model_id_code",
            columnNames = ["car_model_id", "code"],
        ),
    ],
)
class CarGeneration(
    carModelId: Long,
    code: String,
    startYear: Int,
    endYear: Int? = null,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "car_model_id", nullable = false, updatable = false)
    var carModelId: Long = carModelId
        protected set

    @Column(name = "code", nullable = false, length = 50)
    var code: String = code
        protected set

    @Column(name = "start_year", nullable = false)
    var startYear: Int = startYear
        protected set

    // null 이면 현재 판매 중
    @Column(name = "end_year")
    var endYear: Int? = endYear
        protected set

    // 보유 차량 연식은 세대의 판매 기간 안에서만 고를 수 있다 (판매 중이면 올해 + 1 까지)
    fun coversYear(year: Int, currentYear: Int): Boolean {
        val lastYear = endYear ?: (currentYear + 1)
        return year >= startYear && year <= lastYear
    }
}
