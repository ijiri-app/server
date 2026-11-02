package ijiri.ijiriserver.domain.carmodel.application.port.out

import ijiri.ijiriserver.domain.carmodel.entity.CarTrim

interface CarTrimPort {

    fun findAllByCarGenerationIdInOrderByIdAsc(carGenerationIds: Collection<Long>): List<CarTrim>

    fun existsByCarGenerationIdAndName(carGenerationId: Long, name: String): Boolean

    fun findById(id: Long): CarTrim?

    fun save(entity: CarTrim): CarTrim
}
