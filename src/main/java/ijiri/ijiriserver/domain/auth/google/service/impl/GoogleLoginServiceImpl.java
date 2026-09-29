package ijiri.ijiriserver.domain.auth.google.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;
import ijiri.ijiriserver.domain.auth.common.service.SocialLoginService;
import ijiri.ijiriserver.domain.auth.google.client.GoogleOAuthClient;
import ijiri.ijiriserver.domain.auth.google.service.GoogleLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleLoginServiceImpl implements GoogleLoginService {

    private final GoogleOAuthClient googleOAuthClient;
    private final SocialLoginService socialLoginService;

    @Override
    public LoginResponse login(String googleIdToken) {
        return socialLoginService.login(googleOAuthClient.getUserInfo(googleIdToken));
    }
}
