package ijiri.ijiriserver.domain.auth.email.application.port.in;

public interface SendResetCodeEmailVerificationUseCase {

    /**
     * 가입 여부와 무관하게 같은 응답. 활성 이메일 회원에게만 코드를 보낸다.
     */
    void sendResetCode(String email);
}
