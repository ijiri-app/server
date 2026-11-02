package ijiri.ijiriserver.domain.auth.token.application.port.`in`

interface RevokeMemberSessionsUseCase {

    fun signOut(memberId: Long)
}
