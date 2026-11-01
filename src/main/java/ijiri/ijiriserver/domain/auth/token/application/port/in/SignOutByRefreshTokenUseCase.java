package ijiri.ijiriserver.domain.auth.token.application.port.in;

public interface SignOutByRefreshTokenUseCase {

    void signOutByRefreshToken(String refreshToken);
}
