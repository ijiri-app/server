package ijiri.ijiriserver.domain.part.adapter.out.persistence

import ijiri.ijiriserver.domain.part.application.port.out.PartCarModelUsagePort
import org.springframework.stereotype.Component

@Component
class PartCarModelUsagePersistenceAdapter(
    private val partCarModelUsageJpaRepository: PartCarModelUsageJpaRepository,
) : PartCarModelUsagePort {

    override fun add(partId: Long, carModelId: Long, delta: Int) {
        partCarModelUsageJpaRepository.add(partId, carModelId, delta)
    }
}
