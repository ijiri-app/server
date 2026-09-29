package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.auth.common.service.SocialUnlinkService;
import ijiri.ijiriserver.domain.auth.token.service.TokenRevokeService;
import ijiri.ijiriserver.domain.interestcar.service.InterestCarRemoveService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberFindService;
import ijiri.ijiriserver.domain.member.service.MemberWithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberWithdrawServiceImpl implements MemberWithdrawService {

    private final MemberFindService memberFindService;
    private final MemberRepository memberRepository;
    private final TokenRevokeService tokenRevokeService;
    private final InterestCarRemoveService interestCarRemoveService;
    private final SocialUnlinkService socialUnlinkService;

    @Override
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberFindService.getById(memberId);
        tokenRevokeService.revokeAll(memberId);
        interestCarRemoveService.removeAll(memberId);
        memberRepository.delete(member);
        // 외부 호출은 마지막에: 실패하면 예외로 위 삭제가 전부 롤백되어 다시 탈퇴를 시도할 수 있다
        socialUnlinkService.unlink(member.getProvider(), member.getProviderMemberId());
    }
}
