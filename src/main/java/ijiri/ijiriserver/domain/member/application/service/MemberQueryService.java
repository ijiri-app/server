package ijiri.ijiriserver.domain.member.application.service;

import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse;
import ijiri.ijiriserver.domain.member.application.port.in.FindActiveMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.FindEmailMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.GetActiveMembersUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.GetMemberProfileUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.GetMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.GetMyProfileUseCase;
import ijiri.ijiriserver.domain.member.application.port.out.MemberPort;
import ijiri.ijiriserver.domain.member.application.port.out.MemberPostCounter;
import ijiri.ijiriserver.domain.member.application.port.out.MemberWishCounter;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import ijiri.ijiriserver.global.security.AdminVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService implements GetMyProfileUseCase,
        GetMemberProfileUseCase,
        GetMemberUseCase,
        FindActiveMemberUseCase,
        GetActiveMembersUseCase,
        FindEmailMemberUseCase,
        AdminVerifier {

    private final MemberPort memberPort;
    private final MemberPostCounter memberPostCounter;
    private final MemberWishCounter memberWishCounter;

    @Override
    public MemberResponse getMe(Long memberId) {
        Member member = getById(memberId);
        return MemberResponse.me(
                member,
                memberPostCounter.countVisiblePosts(memberId),
                memberWishCounter.countReceivedWishes(memberId)
        );
    }

    @Override
    public MemberResponse getProfile(Long memberId) {
        Member member = getById(memberId);
        return MemberResponse.profile(
                member,
                memberPostCounter.countVisiblePosts(memberId),
                memberWishCounter.countReceivedWishes(memberId)
        );
    }

    @Override
    public Member getById(Long memberId) {
        return findActiveMember(memberId)
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
    }

    @Override
    public Optional<Member> findActiveMember(Long memberId) {
        return memberPort.findByIdAndDeletedAtIsNull(memberId);
    }

    @Override
    public List<Member> getActiveMembers(Collection<Long> memberIds) {
        return memberPort.findAllByIdInAndDeletedAtIsNull(memberIds);
    }

    @Override
    public Optional<Member> findEmailMember(String email) {
        return memberPort.findByProviderAndProviderMemberId(Provider.EMAIL, email);
    }

    // 역할을 바꾸면 다시 로그인하지 않아도 바로 반영되도록 관리자 API 요청마다 DB 를 본다
    @Override
    public boolean isAdmin(String memberId) {
        return findActiveMember(Long.valueOf(memberId))
                .map(Member::isAdmin)
                .orElse(false);
    }
}
