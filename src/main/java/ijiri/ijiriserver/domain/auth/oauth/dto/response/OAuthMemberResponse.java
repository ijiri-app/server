package ijiri.ijiriserver.domain.auth.oauth.dto.response;

import ijiri.ijiriserver.domain.member.entity.Member;

public record OAuthMemberResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {

    public static OAuthMemberResponse from(Member member) {
        return new OAuthMemberResponse(member.getId(), member.getNickname(), member.getProfileImageUrl());
    }
}
