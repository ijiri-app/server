package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.auth.token.service.TokenRevokeService;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberQueryService;
import ijiri.ijiriserver.domain.member.service.MemberWithdrawService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberQueryService, MemberWithdrawService {

    private final MemberRepository memberRepository;
    private final TokenRevokeService tokenRevokeService;

    @Override
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findMember(memberId));
    }

    @Override
    @Transactional
    public void withdraw(Long memberId) {
        Member member = findMember(memberId);
        tokenRevokeService.revokeAll(memberId);
        memberRepository.delete(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
    }
}
