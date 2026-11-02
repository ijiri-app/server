package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence

import ijiri.ijiriserver.domain.carmodel.application.port.out.CarGenerationPort
import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CarGenerationPersistenceAdapter(
    private val carGenerationJpaRepository: CarGenerationJpaRepository,
) : CarGenerationPort {

    override fun findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId: Long): List<CarGeneration> {
        return carGenerationJpaRepository.findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId)
    }

    override fun findByCarModelIdAndCode(carModelId: Long, code: String): CarGeneration? {
        return carGenerationJpaRepository.findByCarModelIdAndCode(carModelId, code)
    }

    override fun findById(id: Long): CarGeneration? {
        return carGenerationJpaRepository.findByIdOrNull(id)
    }

    override fun save(entity: CarGeneration): CarGeneration {
        return carGenerationJpaRepository.save(entity)
    }
}
