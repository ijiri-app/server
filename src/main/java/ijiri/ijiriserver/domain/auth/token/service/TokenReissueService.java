package ijiri.ijiriserver.domain.auth.token.service;

import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;

/**
 * refresh token 으로 토큰 재발급 (자동 로그인)
 */
public interface TokenReissueService {

    TokenResponse reissue(String refreshToken);
}
