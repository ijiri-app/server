package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.entity.Member

interface FindEmailMemberUseCase {

    fun findEmailMember(email: String): Member?
}
