package ijiri.ijiriserver.domain.member.dto;

import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * 소셜 로그인으로 들어온 회원 정보. nickname/profileImageUrl 은 선택 동의라 null 일 수 있다.
 */
public record MemberRegisterCommand(
        Provider provider,
        String providerMemberId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
