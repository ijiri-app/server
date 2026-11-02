package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.entity.Member

interface GetMemberUseCase {

    fun getById(memberId: Long): Member
}
