package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.application.data.MemberSignupCommand
import ijiri.ijiriserver.user.member.entity.Member

interface CreateEmailMemberUseCase {

    fun signup(command: MemberSignupCommand): Member
}
