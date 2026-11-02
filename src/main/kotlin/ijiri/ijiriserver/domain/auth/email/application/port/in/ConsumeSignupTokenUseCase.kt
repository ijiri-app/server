package ijiri.ijiriserver.domain.auth.email.application.port.`in`

interface ConsumeSignupTokenUseCase {

    /**
     * verificationToken 을 확인하고 인증 기록을 삭제한다. 한 번의 인증은 한 번의 가입에만 쓸 수 있다.
     */
    fun consumeSignupToken(email: String, verificationToken: String)
}
