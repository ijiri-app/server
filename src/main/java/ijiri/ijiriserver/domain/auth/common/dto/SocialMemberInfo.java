package ijiri.ijiriserver.domain.auth.common.dto;

import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * 소셜 서버에서 검증 후 꺼낸 유저 정보. 닉네임/프로필 사진은 선택 동의라 null 일 수 있다.
 */
public record SocialMemberInfo(
        Provider provider,
        String providerMemberId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
