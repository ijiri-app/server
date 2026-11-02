package ijiri.ijiriserver.domain.auth.token.application.port.`in`

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse

interface RefreshTokenUseCase {

    fun refresh(refreshToken: String): AuthResponse
}
