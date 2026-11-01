package ijiri.ijiriserver.domain.member.application.data;

import ijiri.ijiriserver.domain.member.entity.Member;

public record MemberRegisterResult(
        Member member,
        boolean isNewMember
) {
}
