package ijiri.ijiriserver.global.security;

/**
 * 관리자 API 접근 시 요청마다 DB 의 현재 역할을 확인한다 (토큰의 role 클레임은 믿지 않는다).
 * 구현은 회원 저장소를 가진 도메인이 제공한다.
 */
public interface AdminVerifier {

    boolean isAdmin(String memberId);
}
