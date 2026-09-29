package ijiri.ijiriserver.domain.auth.common.client;

import ijiri.ijiriserver.domain.auth.common.dto.SocialMemberInfo;
import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * provider 별 소셜 토큰 검증. 새 provider 는 이 인터페이스 구현체만 추가하면 로그인에 연결된다.
 */
public interface SocialTokenVerifier {

    Provider provider();

    SocialMemberInfo verify(String token);
}
