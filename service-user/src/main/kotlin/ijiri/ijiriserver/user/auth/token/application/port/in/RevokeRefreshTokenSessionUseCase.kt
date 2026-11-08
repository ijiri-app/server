package ijiri.ijiriserver.user.auth.token.application.port.`in`

interface RevokeRefreshTokenSessionUseCase {

    fun signOutByRefreshToken(refreshToken: String)
}
