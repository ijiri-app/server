package ijiri.ijiriserver.domain.auth.email.application.port.`in`

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.domain.auth.email.application.data.request.SignInRequest

interface EmailSignInUseCase {

    fun signIn(request: SignInRequest): AuthResponse
}
