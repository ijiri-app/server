package ijiri.ijiriserver.user.auth.email.application.port.`in`

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse

interface VerifySignupCodeUseCase {

    /**
     * 가입 코드가 맞으면 verificationToken(10분, 1회용)을 발급한다.
     */
    fun verifySignupCode(email: String, code: String): AuthResponse
}
