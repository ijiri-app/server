package ijiri.ijiriserver.domain.auth.email.application.port.in;

public interface SignOutWithRefreshTokenUseCase {

    void signOutByRefreshToken(String refreshToken);
}
