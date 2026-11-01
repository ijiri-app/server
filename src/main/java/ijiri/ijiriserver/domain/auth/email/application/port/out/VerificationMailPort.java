package ijiri.ijiriserver.domain.auth.email.application.port.out;

public interface VerificationMailPort {

    void sendCode(String to, String code, long validMinutes);

    void sendAlreadyRegistered(String to);

    void sendWithdrawnAccount(String to);
}
