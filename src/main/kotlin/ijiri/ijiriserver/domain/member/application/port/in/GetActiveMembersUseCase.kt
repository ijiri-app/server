package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.entity.Member

interface GetActiveMembersUseCase {

    fun getActiveMembers(memberIds: Collection<Long>): List<Member>
}
