package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.auth.common.service.SocialUnlinkService;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private static final String DEFAULT_NICKNAME_PREFIX = "이지리오너";

    private final MemberRepository memberRepository;
    private final SocialUnlinkService socialUnlinkService;
    private final ApplicationEventPublisher eventPublisher;

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

    // 이메일 회원은 provider = EMAIL, providerMemberId = email 로 저장해 (provider, providerMemberId) 유니크로 중복을 막는다
    @Override
    @Transactional
    public Member signup(MemberSignupCommand command) {
        if (existsEmailMember(command.email())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }
        return memberRepository.save(Member.builder()
                .provider(Provider.EMAIL)
                .providerMemberId(command.email())
                .email(command.email())
                .nickname(command.nickname())
                .password(command.encodedPassword())
                .role(Role.USER)
                .build()
        );
    }

    @Override
    public Optional<Member> findEmailMember(String email) {
        return memberRepository.findByProviderAndProviderMemberId(Provider.EMAIL, email);
    }

    @Override
    public boolean existsEmailMember(String email) {
        return memberRepository.existsByProviderAndProviderMemberId(Provider.EMAIL, email);
    }

    @Override
    @Transactional
    public void withdraw(Long memberId) {
        Member member = getById(memberId);
        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId));
        memberRepository.delete(member);
        // 외부 호출은 마지막에: 실패하면 예외로 위 삭제가 전부 롤백되어 다시 탈퇴를 시도할 수 있다
        socialUnlinkService.unlink(member.getProvider(), member.getProviderMemberId());
    }

    private Member toMember(MemberRegisterCommand command) {
        return Member.builder()
                .provider(command.provider())
                .providerMemberId(command.providerMemberId())
                .email(command.email())
                .nickname(resolveNickname(command.nickname()))
                .profileImageUrl(command.profileImageUrl())
                .role(Role.USER)
                .build();
    }

    // nickname 은 선택 동의라 없을 수 있다. 가입 단계에서 입력받지 않도록 기본값을 만든다
    private String resolveNickname(String nickname) {
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        return DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
