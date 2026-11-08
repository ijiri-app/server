package ijiri.ijiriserver.user.auth.email.application.port.`in`

interface ConsumeResetCodeUseCase {

    /**
     * 재설정 코드가 맞으면 인증 기록을 삭제한다. 틀린 시도는 횟수에 남는다.
     */
    fun consumeResetCode(email: String, code: String)
}
