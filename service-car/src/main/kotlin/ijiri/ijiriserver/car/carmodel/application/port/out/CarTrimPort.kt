package ijiri.ijiriserver.car.carmodel.application.port.out

import ijiri.ijiriserver.car.carmodel.entity.CarTrim

interface CarTrimPort {

    fun findAllByCarGenerationIdInOrderByIdAsc(carGenerationIds: Collection<Long>): List<CarTrim>

    fun existsByCarGenerationIdAndName(carGenerationId: Long, name: String): Boolean

    fun findAllById(ids: Iterable<Long>): List<CarTrim>

    fun findById(id: Long): CarTrim?

    fun save(entity: CarTrim): CarTrim
}
