package ijiri.ijiriserver.domain.auth.oauth.service;

import ijiri.ijiriserver.domain.auth.oauth.dto.request.OAuthSignInRequest;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;

public interface OAuthService {

    AuthResponse signIn(OAuthSignInRequest request);
}
