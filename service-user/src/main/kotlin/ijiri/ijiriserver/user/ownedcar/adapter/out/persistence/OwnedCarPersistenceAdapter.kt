package ijiri.ijiriserver.user.ownedcar.adapter.out.persistence

import ijiri.ijiriserver.user.ownedcar.application.port.out.OwnedCarPort
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCar
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCarStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class OwnedCarPersistenceAdapter(
    private val ownedCarJpaRepository: OwnedCarJpaRepository,
) : OwnedCarPort {

    override fun findAllByMemberIdOrderByIdAsc(memberId: Long): List<OwnedCar> {
        return ownedCarJpaRepository.findAllByMemberIdOrderByIdAsc(memberId)
    }

    override fun findByIdAndMemberId(id: Long, memberId: Long): OwnedCar? {
        return ownedCarJpaRepository.findByIdAndMemberId(id, memberId)
    }

    override fun countByMemberIdAndStatus(memberId: Long, status: OwnedCarStatus): Long {
        return ownedCarJpaRepository.countByMemberIdAndStatus(memberId, status)
    }

    override fun deleteAllByMemberIdInBulk(memberId: Long) {
        ownedCarJpaRepository.deleteAllByMemberIdInBulk(memberId)
    }

    override fun delete(entity: OwnedCar) {
        ownedCarJpaRepository.delete(entity)
    }

    override fun findAllById(ids: Iterable<Long>): List<OwnedCar> {
        return ownedCarJpaRepository.findAllById(ids)
    }

    override fun findById(id: Long): OwnedCar? {
        return ownedCarJpaRepository.findByIdOrNull(id)
    }

    override fun save(entity: OwnedCar): OwnedCar {
        return ownedCarJpaRepository.save(entity)
    }
}
