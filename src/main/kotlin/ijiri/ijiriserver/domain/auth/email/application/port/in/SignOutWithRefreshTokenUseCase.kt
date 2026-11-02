package ijiri.ijiriserver.domain.auth.email.application.port.`in`

interface SignOutWithRefreshTokenUseCase {

    fun signOutByRefreshToken(refreshToken: String)
}
