package ijiri.ijiriserver.domain.auth.common.service;

import ijiri.ijiriserver.domain.auth.common.dto.SocialUserInfo;
import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;

/**
 * 소셜 검증이 끝난 유저 정보로 회원 조회/가입 후 토큰 발급. provider 별 로그인 서비스가 공통으로 사용.
 */
public interface SocialLoginService {

    LoginResponse login(SocialUserInfo userInfo);
}
