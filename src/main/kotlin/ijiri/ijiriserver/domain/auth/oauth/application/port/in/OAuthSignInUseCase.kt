package ijiri.ijiriserver.domain.auth.oauth.application.port.`in`

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.domain.auth.oauth.application.data.request.OAuthSignInRequest

interface OAuthSignInUseCase {

    fun signIn(request: OAuthSignInRequest): AuthResponse
}
