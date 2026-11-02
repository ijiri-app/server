package ijiri.ijiriserver.domain.member.application.port.`in`

interface PurgeMemberUseCase {

    fun purge(memberId: Long)
}
