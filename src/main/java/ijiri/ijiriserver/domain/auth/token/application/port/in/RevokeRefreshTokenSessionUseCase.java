package ijiri.ijiriserver.domain.auth.token.application.port.in;

public interface RevokeRefreshTokenSessionUseCase {

    void signOutByRefreshToken(String refreshToken);
}
