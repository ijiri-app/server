package ijiri.ijiriserver.domain.auth.common.dto.response;

import ijiri.ijiriserver.domain.member.entity.Member;

public record SignInMemberResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {

    public static SignInMemberResponse from(Member member) {
        return new SignInMemberResponse(member.getId(), member.getNickname(), member.getProfileImageUrl());
    }
}
