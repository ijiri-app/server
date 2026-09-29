package ijiri.ijiriserver.domain.member.service;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

public interface MemberService {

    MemberResponse getMember(Long memberId);

    Member getById(Long memberId);

    MemberRegisterResult registerIfAbsent(MemberRegisterCommand command);

    void withdraw(Long memberId);
}
