package ijiri.ijiriserver.car.carmodel.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 차종 마스터의 3단계(트림).
 */
@Entity
@Table(
    name = "car_trim",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_car_trim_car_generation_id_name",
            columnNames = ["car_generation_id", "name"],
        ),
    ],
)
class CarTrim(
    carGenerationId: Long,
    name: String,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "car_generation_id", nullable = false, updatable = false)
    var carGenerationId: Long = carGenerationId
        protected set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        protected set
}
