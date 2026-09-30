package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private static final String DEFAULT_NICKNAME_PREFIX = "이지리오너";
    // 이메일 가입과 같은 닉네임 규칙(2~12자)을 소셜 닉네임에도 적용한다
    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 12;
    // 탈퇴 후 회원 행과 데이터를 보관하는 기간. 이 기간 동안은 같은 계정으로 재가입할 수 없다
    private static final long WITHDRAWAL_RETENTION_DAYS = 30;

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(getById(memberId));
    }

    @Override
    public Member getById(Long memberId) {
        return findActiveMember(memberId)
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
    }

    @Override
    public Optional<Member> findActiveMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId);
    }

    @Override
    @Transactional
    public MemberRegisterResult registerIfAbsent(MemberRegisterCommand command) {
        return memberRepository.findByProviderAndProviderMemberId(command.provider(), command.providerMemberId())
                .map(member -> {
                    if (member.isWithdrawn()) {
                        throw new CustomException(MemberStatusCode.MEMBER_WITHDRAWN);
                    }
                    return new MemberRegisterResult(member, false);
                })
                .orElseGet(() -> new MemberRegisterResult(memberRepository.save(toMember(command)), true));
    }

    // 이메일 회원은 provider = EMAIL, providerMemberId = email 로 저장해
    // (provider, providerMemberId) 유니크로 중복을 막는다. 동시 가입 경합도 유니크 위반으로 걸러진다
    @Override
    @Transactional
    public Member signup(MemberSignupCommand command) {
        if (existsEmailMember(command.email())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }
        try {
            return memberRepository.saveAndFlush(Member.builder()
                    .provider(Provider.EMAIL)
                    .providerMemberId(command.email())
                    .email(command.email())
                    .nickname(command.nickname())
                    .password(command.encodedPassword())
                    .role(Role.USER)
                    .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }
    }

    @Override
    public Optional<Member> findEmailMember(String email) {
        return memberRepository.findByProviderAndProviderMemberId(Provider.EMAIL, email);
    }

    // soft delete: 행은 보관 기간 동안 남기고, 즉시 처리할 일(토큰 폐기, 소셜 연결 끊기 등)은 이벤트로 넘긴다.
    // 소셜 연결 끊기는 커밋 직전에 실행되어, 실패하면 탈퇴 전체가 롤백되고 다시 시도할 수 있다
    @Override
    @Transactional
    public MemberResponse withdraw(Long memberId) {
        Member member = getById(memberId);
        member.withdraw(LocalDateTime.now(clock));
        eventPublisher.publishEvent(new MemberWithdrawnEvent(
                memberId,
                member.getProvider(),
                member.getProviderMemberId()
        ));
        return MemberResponse.from(member);
    }

    @Override
    public List<Long> findPurgeTargetIds() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(WITHDRAWAL_RETENTION_DAYS);
        return memberRepository.findIdsWithdrawnBefore(cutoff);
    }

    // 다른 도메인이 자기 데이터를 먼저 지우도록 이벤트를 발행한 뒤 회원 행을 삭제한다
    @Override
    @Transactional
    public void purge(Long memberId) {
        memberRepository.findById(memberId)
                .filter(Member::isWithdrawn)
                .ifPresent(member -> {
                    eventPublisher.publishEvent(new MemberPurgedEvent(
                            memberId,
                            member.getProvider(),
                            member.getProviderMemberId()
                    ));
                    memberRepository.delete(member);
                });
    }

    private boolean existsEmailMember(String email) {
        return memberRepository.existsByProviderAndProviderMemberId(Provider.EMAIL, email);
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

    // 소셜 닉네임은 선택 동의라 없거나 규칙보다 짧거나 길 수 있다.
    // 가입 단계에서 입력받지 않도록 긴 닉네임은 자르고, 없거나 짧으면 기본값을 만든다
    private String resolveNickname(String nickname) {
        String trimmed = nickname == null ? "" : nickname.strip();
        if (trimmed.codePointCount(0, trimmed.length()) < MIN_NICKNAME_LENGTH) {
            return DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return trimmed.codePoints()
                .limit(MAX_NICKNAME_LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
