package ijiri.ijiriserver.domain.member.dto;

import ijiri.ijiriserver.domain.member.entity.Member;

public record MemberRegisterResult(
        Member member,
        boolean isNewMember
) {
}
