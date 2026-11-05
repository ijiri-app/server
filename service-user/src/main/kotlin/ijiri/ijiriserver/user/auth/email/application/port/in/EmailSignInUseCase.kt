package ijiri.ijiriserver.user.auth.email.application.port.`in`

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.email.application.data.request.SignInRequest

interface EmailSignInUseCase {

    fun signIn(request: SignInRequest): AuthResponse
}
