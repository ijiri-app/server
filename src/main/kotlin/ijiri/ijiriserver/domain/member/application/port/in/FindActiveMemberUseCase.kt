package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.entity.Member

interface FindActiveMemberUseCase {

    fun findActiveMember(memberId: Long): Member?
}
