package ijiri.ijiriserver.user.interestcar.application.port.out

import ijiri.ijiriserver.user.interestcar.entity.MemberInterestCar

interface MemberInterestCarPort {

    fun findAllByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<MemberInterestCar>

    fun deleteAllByMemberIdInBulk(memberId: Long)

    fun saveAll(entities: Iterable<MemberInterestCar>): List<MemberInterestCar>
}
