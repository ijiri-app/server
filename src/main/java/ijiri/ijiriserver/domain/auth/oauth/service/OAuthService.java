package ijiri.ijiriserver.domain.auth.oauth.service;

import ijiri.ijiriserver.domain.auth.oauth.dto.request.OAuthSignInRequest;
import ijiri.ijiriserver.domain.auth.common.dto.response.SignInResponse;

public interface OAuthService {

    SignInResponse signIn(OAuthSignInRequest request);
}
