package ijiri.ijiriserver.user.auth.email.application.port.`in`

import ijiri.ijiriserver.user.auth.email.application.data.request.PasswordResetRequest

interface ResetPasswordUseCase {

    fun resetPassword(request: PasswordResetRequest)
}
