package ijiri.ijiriserver.car.carmodel.application.port.out

import ijiri.ijiriserver.car.carmodel.entity.CarGeneration

interface CarGenerationPort {

    fun findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId: Long): List<CarGeneration>

    fun findByCarModelIdAndCode(carModelId: Long, code: String): CarGeneration?

    fun findAllById(ids: Iterable<Long>): List<CarGeneration>

    fun findById(id: Long): CarGeneration?

    fun save(entity: CarGeneration): CarGeneration
}
