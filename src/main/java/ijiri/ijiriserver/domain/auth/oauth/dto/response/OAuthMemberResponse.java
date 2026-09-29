package ijiri.ijiriserver.domain.auth.oauth.dto.response;

import ijiri.ijiriserver.domain.member.entity.Member;

public record OAuthMemberResponse(
        Long id,
        String username,
        String profileImageUrl
) {

    public static OAuthMemberResponse from(Member member) {
        return new OAuthMemberResponse(member.getId(), member.getUsername(), member.getProfileImageUrl());
    }
}
