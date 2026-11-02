package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.entity.Member

interface FindActiveMemberUseCase {

    fun findActiveMember(memberId: Long): Member?
}
