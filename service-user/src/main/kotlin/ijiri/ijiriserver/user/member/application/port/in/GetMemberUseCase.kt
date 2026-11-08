package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.entity.Member

interface GetMemberUseCase {

    fun getById(memberId: Long): Member
}
