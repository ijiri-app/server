package ijiri.ijiriserver.global.jwt;

/**
 * access token 의 로그인 세션(sid)이 아직 유효한지 확인한다.
 * JWT 는 서버가 지울 수 없으므로, 짝이 되는 refresh token 이 삭제되면(로그아웃, 다른 기기 로그인, 탈퇴)
 * 같은 세션의 access token 도 즉시 거부하기 위해 쓴다. 구현은 토큰 저장소를 가진 도메인이 제공한다.
 */
public interface SessionValidator {

    boolean isActive(String sessionId);
}
