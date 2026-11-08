package ijiri.ijiriserver.car.carmodel.application.port.out

import ijiri.ijiriserver.car.carmodel.entity.CarModel

interface CarModelPort {

    fun search(keyword: String, coreOnly: Boolean): List<CarModel>

    fun findByIdAndHiddenFalse(id: Long): CarModel?

    fun findByBrandAndName(brand: String, name: String): CarModel?

    fun countByIdInAndHiddenFalse(ids: Collection<Long>): Long

    fun count(): Long

    fun findAllById(ids: Iterable<Long>): List<CarModel>

    fun findById(id: Long): CarModel?

    fun save(entity: CarModel): CarModel
}
