package ijiri.ijiriserver.domain.interestcar.adapter.out.persistence

import ijiri.ijiriserver.domain.interestcar.application.port.out.MemberInterestCarPort
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar
import org.springframework.stereotype.Component

@Component
class MemberInterestCarPersistenceAdapter(
    private val memberInterestCarJpaRepository: MemberInterestCarJpaRepository,
) : MemberInterestCarPort {

    override fun findAllByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<MemberInterestCar> {
        return memberInterestCarJpaRepository.findAllByMemberIdOrderByDisplayOrderAsc(memberId)
    }

    override fun deleteAllByMemberIdInBulk(memberId: Long) {
        memberInterestCarJpaRepository.deleteAllByMemberIdInBulk(memberId)
    }

    override fun saveAll(entities: Iterable<MemberInterestCar>): List<MemberInterestCar> {
        return memberInterestCarJpaRepository.saveAll(entities)
    }
}
