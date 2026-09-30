package ijiri.ijiriserver.domain.member.service;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberService {

    MemberResponse getMember(Long memberId);

    Member getById(Long memberId);

    MemberRegisterResult registerIfAbsent(MemberRegisterCommand command);

    Member signup(MemberSignupCommand command);

    Optional<Member> findEmailMember(String email);

    MemberResponse withdraw(Long memberId);

    List<Long> findPurgeTargetIds();

    void purge(Long memberId);
}
