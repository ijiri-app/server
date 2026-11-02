package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration
import org.springframework.data.jpa.repository.JpaRepository

interface CarGenerationJpaRepository : JpaRepository<CarGeneration, Long> {

    fun findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId: Long): List<CarGeneration>

    fun findByCarModelIdAndCode(carModelId: Long, code: String): CarGeneration?
}
