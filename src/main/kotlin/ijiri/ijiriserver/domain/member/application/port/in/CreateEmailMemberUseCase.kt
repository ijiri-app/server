package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.application.data.MemberSignupCommand
import ijiri.ijiriserver.domain.member.entity.Member

interface CreateEmailMemberUseCase {

    fun signup(command: MemberSignupCommand): Member
}
