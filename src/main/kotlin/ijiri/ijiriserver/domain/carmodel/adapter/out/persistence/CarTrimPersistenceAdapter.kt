package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence

import ijiri.ijiriserver.domain.carmodel.application.port.out.CarTrimPort
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CarTrimPersistenceAdapter(
    private val carTrimJpaRepository: CarTrimJpaRepository,
) : CarTrimPort {

    override fun findAllByCarGenerationIdInOrderByIdAsc(carGenerationIds: Collection<Long>): List<CarTrim> {
        return carTrimJpaRepository.findAllByCarGenerationIdInOrderByIdAsc(carGenerationIds)
    }

    override fun existsByCarGenerationIdAndName(carGenerationId: Long, name: String): Boolean {
        return carTrimJpaRepository.existsByCarGenerationIdAndName(carGenerationId, name)
    }

    override fun findById(id: Long): CarTrim? {
        return carTrimJpaRepository.findByIdOrNull(id)
    }

    override fun save(entity: CarTrim): CarTrim {
        return carTrimJpaRepository.save(entity)
    }
}
