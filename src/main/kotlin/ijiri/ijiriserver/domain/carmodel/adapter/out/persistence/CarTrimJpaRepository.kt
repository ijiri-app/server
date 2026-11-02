package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence

import ijiri.ijiriserver.domain.carmodel.entity.CarTrim
import org.springframework.data.jpa.repository.JpaRepository

interface CarTrimJpaRepository : JpaRepository<CarTrim, Long> {

    fun findAllByCarGenerationIdInOrderByIdAsc(carGenerationIds: Collection<Long>): List<CarTrim>

    fun existsByCarGenerationIdAndName(carGenerationId: Long, name: String): Boolean
}
