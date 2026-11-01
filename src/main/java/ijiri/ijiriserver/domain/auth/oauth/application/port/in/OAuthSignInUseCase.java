package ijiri.ijiriserver.domain.auth.oauth.application.port.in;

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.oauth.application.data.request.OAuthSignInRequest;

public interface OAuthSignInUseCase {

    AuthResponse signIn(OAuthSignInRequest request);
}
