package ijiri.ijiriserver.domain.auth.common.client;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * provider 별 소셜 토큰 검증. 새 provider 는 이 인터페이스 구현체만 추가하면 로그인에 연결된다.
 * 검증한 회원 정보를 그대로 가입/조회에 쓸 수 있는 형태로 돌려준다.
 */
public interface SocialTokenVerifier {

    Provider provider();

    MemberRegisterCommand verify(String token);
}
