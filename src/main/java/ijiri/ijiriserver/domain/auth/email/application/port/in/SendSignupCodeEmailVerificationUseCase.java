package ijiri.ijiriserver.domain.auth.email.application.port.in;

public interface SendSignupCodeEmailVerificationUseCase {

    /**
     * 가입 여부와 무관하게 같은 응답. 이미 가입된 이메일이면 코드 대신 안내 메일을 보낸다.
     */
    void sendSignupCode(String email);
}
