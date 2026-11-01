package ijiri.ijiriserver.domain.auth.email.application.port.in;

public interface ConsumeResetCodeEmailVerificationUseCase {

    /**
     * 재설정 코드가 맞으면 인증 기록을 삭제한다. 틀린 시도는 횟수에 남는다.
     */
    void consumeResetCode(String email, String code);
}
