package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence

import ijiri.ijiriserver.domain.carmodel.application.port.out.CarModelPort
import ijiri.ijiriserver.domain.carmodel.entity.CarModel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CarModelPersistenceAdapter(
    private val carModelJpaRepository: CarModelJpaRepository,
) : CarModelPort {

    override fun search(keyword: String, coreOnly: Boolean): List<CarModel> {
        return carModelJpaRepository.search(keyword, coreOnly)
    }

    override fun findByIdAndHiddenFalse(id: Long): CarModel? {
        return carModelJpaRepository.findByIdAndHiddenFalse(id)
    }

    override fun findByBrandAndName(brand: String, name: String): CarModel? {
        return carModelJpaRepository.findByBrandAndName(brand, name)
    }

    override fun countByIdInAndHiddenFalse(ids: Collection<Long>): Long {
        return carModelJpaRepository.countByIdInAndHiddenFalse(ids)
    }

    override fun count(): Long {
        return carModelJpaRepository.count()
    }

    override fun findAllById(ids: Iterable<Long>): List<CarModel> {
        return carModelJpaRepository.findAllById(ids)
    }

    override fun findById(id: Long): CarModel? {
        return carModelJpaRepository.findByIdOrNull(id)
    }

    override fun save(entity: CarModel): CarModel {
        return carModelJpaRepository.save(entity)
    }
}
