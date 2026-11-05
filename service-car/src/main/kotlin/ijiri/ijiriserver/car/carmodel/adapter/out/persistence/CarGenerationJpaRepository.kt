package ijiri.ijiriserver.car.carmodel.adapter.out.persistence

import ijiri.ijiriserver.car.carmodel.entity.CarGeneration
import org.springframework.data.jpa.repository.JpaRepository

interface CarGenerationJpaRepository : JpaRepository<CarGeneration, Long> {

    fun findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId: Long): List<CarGeneration>

    fun findByCarModelIdAndCode(carModelId: Long, code: String): CarGeneration?
}
