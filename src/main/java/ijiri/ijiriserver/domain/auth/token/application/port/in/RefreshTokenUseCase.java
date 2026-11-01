package ijiri.ijiriserver.domain.auth.token.application.port.in;

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse;

public interface RefreshTokenUseCase {

    AuthResponse refresh(String refreshToken);
}
