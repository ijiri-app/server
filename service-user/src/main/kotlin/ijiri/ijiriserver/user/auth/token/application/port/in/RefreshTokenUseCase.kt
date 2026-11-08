package ijiri.ijiriserver.user.auth.token.application.port.`in`

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse

interface RefreshTokenUseCase {

    fun refresh(refreshToken: String): AuthResponse
}
