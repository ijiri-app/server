package ijiri.ijiriserver.domain.member.service;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.request.MemberUpdateRequest;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberService {

    MemberResponse getMember(Long memberId);

    Member getById(Long memberId);

    Optional<Member> findActiveMember(Long memberId);

    List<Member> getActiveMembers(Collection<Long> memberIds);

    MemberResponse updateProfile(Long memberId, MemberUpdateRequest request);

    MemberRegisterResult registerIfAbsent(MemberRegisterCommand command);

    Member signup(MemberSignupCommand command);

    Optional<Member> findEmailMember(String email);

    /**
     * 활성 이메일 회원의 비밀번호를 바꾸고 회원 id 를 돌려준다. 없으면 MEMBER404.
     */
    Long changePassword(String email, String encodedPassword);

    MemberResponse withdraw(Long memberId);

    /**
     * until 까지 이용을 정지하고 세션을 끊는다. 이미 더 긴 정지가 걸려 있어도 until 로 덮어쓴다.
     */
    void suspend(Long memberId, LocalDateTime until);

    List<Long> findPurgeTargetIds();

    void purge(Long memberId);
}
