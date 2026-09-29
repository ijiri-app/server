package ijiri.ijiriserver.domain.auth.common.dto;

import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * 소셜 서버에서 검증 후 꺼낸 유저 정보. 유저 식별은 provider + providerUserId 조합.
 */
public record SocialUserInfo(
        Provider provider,
        String providerUserId,
        String email,
        String nickname
) {
}
