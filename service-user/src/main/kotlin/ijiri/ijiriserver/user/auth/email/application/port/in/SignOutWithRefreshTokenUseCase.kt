package ijiri.ijiriserver.user.auth.email.application.port.`in`

interface SignOutWithRefreshTokenUseCase {

    fun signOutByRefreshToken(refreshToken: String)
}
