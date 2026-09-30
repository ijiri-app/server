package ijiri.ijiriserver.domain.auth.common.client;

import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * 회원 탈퇴 시 소셜 계정 연결 끊기가 필요한 provider 만 구현한다.
 */
public interface SocialUnlinkClient {

    Provider provider();

    void unlink(String providerMemberId);
}
