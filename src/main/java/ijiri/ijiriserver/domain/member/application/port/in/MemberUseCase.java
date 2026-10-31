package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.application.data.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.application.data.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.application.data.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.application.data.request.MemberUpdateRequest;
import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberUseCase {

    MemberResponse getMe(Long memberId);

    MemberResponse getProfile(Long memberId);

    Member getById(Long memberId);

    Optional<Member> findActiveMember(Long memberId);

    List<Member> getActiveMembers(Collection<Long> memberIds);

    MemberResponse updateProfile(Long memberId, MemberUpdateRequest request);

    MemberRegisterResult registerIfAbsent(MemberRegisterCommand command);

    Member signup(MemberSignupCommand command);

    Optional<Member> findEmailMember(String email);

    /**
     * 활성 이메일 회원의 비밀번호를 바꾸고 회원 id 를 돌려준다. 없으면 NOT_FOUND.
     */
    Long changePassword(String email, String encodedPassword);

    void withdraw(Long memberId);

    /**
     * until 까지 이용을 정지하고 세션을 끊는다. 이미 더 긴 정지가 걸려 있어도 until 로 덮어쓴다.
     * 이미 탈퇴한 회원이면 아무것도 하지 않는다 (신고 처리가 막히지 않도록).
     */
    void suspend(Long memberId, LocalDateTime until);

    /**
     * 보관 기간이 지난 탈퇴 회원 id 를 afterId 다음부터 size 개.
     */
    List<Long> findPurgeTargetIds(Long afterId, int size);

    void purge(Long memberId);
}
