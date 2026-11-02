package ijiri.ijiriserver.domain.auth.email.application.port.`in`

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse

interface VerifySignupCodeUseCase {

    /**
     * 가입 코드가 맞으면 verificationToken(10분, 1회용)을 발급한다.
     */
    fun verifySignupCode(email: String, code: String): AuthResponse
}
