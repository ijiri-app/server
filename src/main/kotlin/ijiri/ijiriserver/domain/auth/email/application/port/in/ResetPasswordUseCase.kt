package ijiri.ijiriserver.domain.auth.email.application.port.`in`

import ijiri.ijiriserver.domain.auth.email.application.data.request.PasswordResetRequest

interface ResetPasswordUseCase {

    fun resetPassword(request: PasswordResetRequest)
}
