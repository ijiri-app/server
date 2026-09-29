package ijiri.ijiriserver.domain.auth.token.service;

import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;

/**
 * refresh token 으로 access/refresh token 재발급 (자동 로그인). 기존 refresh token 은 폐기
 */
public interface TokenRefreshService {

    TokenResponse refresh(String refreshToken);
}
