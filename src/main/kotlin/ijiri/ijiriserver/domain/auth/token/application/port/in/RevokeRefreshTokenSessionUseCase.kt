package ijiri.ijiriserver.domain.auth.token.application.port.`in`

interface RevokeRefreshTokenSessionUseCase {

    fun signOutByRefreshToken(refreshToken: String)
}
