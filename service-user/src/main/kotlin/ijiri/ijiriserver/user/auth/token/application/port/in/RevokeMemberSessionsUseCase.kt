package ijiri.ijiriserver.user.auth.token.application.port.`in`

interface RevokeMemberSessionsUseCase {

    fun signOut(memberId: Long)
}
