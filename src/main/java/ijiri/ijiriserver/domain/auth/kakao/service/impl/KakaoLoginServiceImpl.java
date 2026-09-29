package ijiri.ijiriserver.domain.auth.kakao.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;
import ijiri.ijiriserver.domain.auth.common.service.SocialLoginService;
import ijiri.ijiriserver.domain.auth.kakao.client.KakaoOAuthClient;
import ijiri.ijiriserver.domain.auth.kakao.service.KakaoLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoLoginServiceImpl implements KakaoLoginService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final SocialLoginService socialLoginService;

    @Override
    public LoginResponse login(String kakaoAccessToken) {
        return socialLoginService.login(kakaoOAuthClient.getUserInfo(kakaoAccessToken));
    }
}
