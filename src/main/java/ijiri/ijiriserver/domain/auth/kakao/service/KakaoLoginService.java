package ijiri.ijiriserver.domain.auth.kakao.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;

public interface KakaoLoginService {

    LoginResponse login(String kakaoAccessToken);
}
