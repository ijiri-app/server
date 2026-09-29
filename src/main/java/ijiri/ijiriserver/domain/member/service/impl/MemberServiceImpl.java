package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberFindService;
import ijiri.ijiriserver.domain.member.service.MemberQueryService;
import ijiri.ijiriserver.domain.member.service.MemberRegisterService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberQueryService, MemberFindService, MemberRegisterService {

    private static final String DEFAULT_USERNAME_PREFIX = "이지리오너";

    private final MemberRepository memberRepository;

    @Override
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(getById(memberId));
    }

    @Override
    public Member getById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
    }

    @Override
    @Transactional
    public MemberRegisterResult registerIfAbsent(MemberRegisterCommand command) {
        return memberRepository.findByProviderAndProviderMemberId(command.provider(), command.providerMemberId())
                .map(member -> new MemberRegisterResult(member, false))
                .orElseGet(() -> new MemberRegisterResult(memberRepository.save(toMember(command)), true));
    }

    private Member toMember(MemberRegisterCommand command) {
        return Member.builder()
                .provider(command.provider())
                .providerMemberId(command.providerMemberId())
                .email(command.email())
                .username(resolveUsername(command.username()))
                .profileImageUrl(command.profileImageUrl())
                .role(Role.USER)
                .build();
    }

    // username 은 선택 동의라 없을 수 있다. 가입 단계에서 입력받지 않도록 기본값을 만든다
    private String resolveUsername(String username) {
        if (StringUtils.hasText(username)) {
            return username;
        }
        return DEFAULT_USERNAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
