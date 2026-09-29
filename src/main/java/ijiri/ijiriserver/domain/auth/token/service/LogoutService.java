package ijiri.ijiriserver.domain.auth.token.service;

/**
 * 해당 기기의 refresh token 폐기
 */
public interface LogoutService {

    void logout(String refreshToken);
}
