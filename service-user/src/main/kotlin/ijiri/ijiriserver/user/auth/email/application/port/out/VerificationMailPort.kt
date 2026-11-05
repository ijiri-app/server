package ijiri.ijiriserver.user.auth.email.application.port.out

interface VerificationMailPort {

    fun sendCode(to: String, code: String, validMinutes: Long)

    fun sendAlreadyRegistered(to: String)

    fun sendWithdrawnAccount(to: String)
}
