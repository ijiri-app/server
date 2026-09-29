package ijiri.ijiriserver.domain.auth.oauth.service;

import ijiri.ijiriserver.domain.auth.oauth.dto.request.OAuthLoginRequest;
import ijiri.ijiriserver.domain.auth.oauth.dto.response.OAuthLoginResponse;

public interface OAuthService {

    OAuthLoginResponse login(OAuthLoginRequest request);
}
