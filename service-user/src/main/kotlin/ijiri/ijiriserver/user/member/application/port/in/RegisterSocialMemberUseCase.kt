package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.application.data.MemberRegisterCommand
import ijiri.ijiriserver.user.member.application.data.MemberRegisterResult

interface RegisterSocialMemberUseCase {

    fun registerIfAbsent(command: MemberRegisterCommand): MemberRegisterResult
}
