package ijiri.ijiriserver.domain.interestcar.application.port.out

import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar

interface MemberInterestCarPort {

    fun findAllByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<MemberInterestCar>

    fun deleteAllByMemberIdInBulk(memberId: Long)

    fun saveAll(entities: Iterable<MemberInterestCar>): List<MemberInterestCar>
}
