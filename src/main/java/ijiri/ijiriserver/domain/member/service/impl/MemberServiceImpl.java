package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.request.MemberUpdateRequest;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.event.MemberProfileImageChangedEvent;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberSuspendedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberPostCounter;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.member.service.MemberWishCounter;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.security.AdminVerifier;
import ijiri.ijiriserver.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService, AdminVerifier {

    private static final String DEFAULT_NICKNAME_PREFIX = "이지리오너";
    // 이메일 가입과 같은 닉네임 규칙(2~12자)을 소셜 닉네임에도 적용한다
    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 12;
    private static final int NICKNAME_SUFFIX_DIGITS = 4;
    private static final int MAX_NICKNAME_TRIES = 10;
    // 탈퇴 후 회원 행과 데이터를 보관하는 기간. 이 기간 동안은 같은 계정으로 재가입할 수 없다
    private static final long WITHDRAWAL_RETENTION_DAYS = 30;

    private final MemberRepository memberRepository;
    private final MemberPostCounter memberPostCounter;
    private final MemberWishCounter memberWishCounter;
    private final ImageUrlResolver imageUrlResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

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
        return memberRepository.findByIdAndDeletedAtIsNull(memberId);
    }

    @Override
    public List<Member> getActiveMembers(Collection<Long> memberIds) {
        return memberRepository.findAllByIdInAndDeletedAtIsNull(memberIds);
    }

    // 프로필 사진은 업로드 도메인이 이벤트를 받아 이 회원이 올린 사진인지 확인한다 (아니면 예외로 롤백)
    @Override
    @Transactional
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        Member member = getById(memberId);
        String nickname = member.getNickname();
        if (request.nickname() != null && !request.nickname().equals(nickname)) {
            if (memberRepository.existsByNickname(request.nickname())) {
                throw new CustomException(MemberStatusCode.DUPLICATE_NICKNAME);
            }
            nickname = request.nickname();
        }
        String previousUrl = member.getProfileImageUrl();
        String profileImageUrl = previousUrl;
        if (request.profileImageKey() != null) {
            profileImageUrl = request.profileImageKey().isBlank()
                    ? null
                    : imageUrlResolver.urlOf(request.profileImageKey());
        }
        String statusMessage = request.statusMessage() == null
                ? member.getStatusMessage()
                : emptyToNull(request.statusMessage());

        member.updateProfile(nickname, profileImageUrl, statusMessage);
        if (!Objects.equals(previousUrl, profileImageUrl)) {
            eventPublisher.publishEvent(new MemberProfileImageChangedEvent(
                    memberId,
                    previousUrl,
                    profileImageUrl == null ? null : request.profileImageKey()
            ));
        }
        return getMe(memberId);
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
        if (memberRepository.existsByProviderAndProviderMemberId(Provider.EMAIL, command.email())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByNickname(command.nickname())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_NICKNAME);
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

    @Override
    @Transactional
    public Long changePassword(String email, String encodedPassword) {
        Member member = findEmailMember(email)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
        member.changePassword(encodedPassword);
        return member.getId();
    }

    // soft delete: 행은 보관 기간 동안 남기고, 즉시 처리할 일(토큰 폐기, 소셜 연결 끊기, 게시물 비공개 등)은 이벤트로 넘긴다.
    // 소셜 연결 끊기처럼 외부 API 를 부르는 일은 커밋 뒤에 실행되고, 실패하면 영구 삭제 전에 다시 시도한다
    @Override
    @Transactional
    public void withdraw(Long memberId) {
        Member member = getById(memberId);
        member.withdraw(LocalDateTime.now(clock));
        eventPublisher.publishEvent(new MemberWithdrawnEvent(
                memberId,
                member.getProvider(),
                member.getProviderMemberId()
        ));
    }

    @Override
    @Transactional
    public void suspend(Long memberId, LocalDateTime until) {
        getById(memberId).suspend(until);
        eventPublisher.publishEvent(new MemberSuspendedEvent(memberId));
    }

    @Override
    public List<Long> findPurgeTargetIds(Long afterId, int size) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(WITHDRAWAL_RETENTION_DAYS);
        return memberRepository.findIdsWithdrawnBefore(cutoff, afterId, Limit.of(size));
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

    // 역할을 바꾸면 다시 로그인하지 않아도 바로 반영되도록 관리자 API 요청마다 DB 를 본다
    @Override
    public boolean isAdmin(String memberId) {
        return findActiveMember(Long.valueOf(memberId))
                .map(Member::isAdmin)
                .orElse(false);
    }

    private Member toMember(MemberRegisterCommand command) {
        return Member.builder()
                .provider(command.provider())
                .providerMemberId(command.providerMemberId())
                .email(command.email())
                .nickname(uniqueNickname(command.nickname()))
                .profileImageUrl(command.profileImageUrl())
                .role(Role.USER)
                .build();
    }

    // 소셜 닉네임은 선택 동의라 없거나 규칙보다 짧거나 길 수 있고, 다른 회원과 겹칠 수 있다.
    // 가입 단계에서 입력받지 않도록 긴 닉네임은 자르고, 없거나 짧으면 기본값을 쓰고, 겹치면 뒤에 숫자를 붙인다
    private String uniqueNickname(String nickname) {
        String base = baseNickname(nickname);
        if (!memberRepository.existsByNickname(base)) {
            return base;
        }
        String prefix = truncate(base, MAX_NICKNAME_LENGTH - NICKNAME_SUFFIX_DIGITS);
        for (int i = 0; i < MAX_NICKNAME_TRIES; i++) {
            String candidate = prefix + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (!memberRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        // 그래도 겹치면 저장할 때 유니크 제약에 걸려 OAuthService 가 한 번 더 시도한다
        return prefix + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String baseNickname(String nickname) {
        String trimmed = nickname == null ? "" : nickname.strip();
        if (trimmed.codePointCount(0, trimmed.length()) < MIN_NICKNAME_LENGTH) {
            return DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return truncate(trimmed, MAX_NICKNAME_LENGTH);
    }

    private String truncate(String value, int maxCodePoints) {
        return value.codePoints()
                .limit(maxCodePoints)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
