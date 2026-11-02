package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.entity.Member

interface FindEmailMemberUseCase {

    fun findEmailMember(email: String): Member?
}
