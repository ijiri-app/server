package ijiri.ijiriserver.domain.carmodel.application.port.out

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration

interface CarGenerationPort {

    fun findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId: Long): List<CarGeneration>

    fun findByCarModelIdAndCode(carModelId: Long, code: String): CarGeneration?

    fun findById(id: Long): CarGeneration?

    fun save(entity: CarGeneration): CarGeneration
}
