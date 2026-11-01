package ijiri.ijiriserver.domain.auth.email.application.port.in;

public interface SignOutByRefreshTokenAuthUseCase {

    void signOutByRefreshToken(String refreshToken);
}
