package ijiri.ijiriserver.user.auth.oauth.application.port.`in`

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.oauth.application.data.request.OAuthSignInRequest

interface OAuthSignInUseCase {

    fun signIn(request: OAuthSignInRequest): AuthResponse
}
