package ijiri.ijiriserver.domain.auth.token.service;

/**
 * 해당 기기의 refresh token 폐기. 본인 토큰만 폐기된다
 */
public interface LogoutService {

    void logout(Long memberId, String refreshToken);
}
