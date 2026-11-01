package ijiri.ijiriserver.domain.member.application.service;

import ijiri.ijiriserver.domain.member.application.data.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.application.data.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.application.data.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.application.data.request.MemberUpdateRequest;
import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse;
import ijiri.ijiriserver.domain.member.application.port.in.ChangeMemberPasswordUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.CreateEmailMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.FindEmailMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.GetMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.GetMyProfileUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.RegisterSocialMemberUseCase;
import ijiri.ijiriserver.domain.member.application.port.in.UpdateMemberProfileUseCase;
import ijiri.ijiriserver.domain.member.application.port.out.MemberPort;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.event.MemberProfileImageChangedEvent;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import ijiri.ijiriserver.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCommandService implements UpdateMemberProfileUseCase,
        RegisterSocialMemberUseCase,
        CreateEmailMemberUseCase,
        ChangeMemberPasswordUseCase {

    private static final String DEFAULT_NICKNAME_PREFIX = "이지리오너";
    private static final int MAX_NICKNAME_TRIES = 10;

    private final MemberPort memberPort;
    private final ImageUrlResolver imageUrlResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final GetMemberUseCase getMemberUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final FindEmailMemberUseCase findEmailMemberUseCase;

    // 프로필 사진은 업로드 도메인이 이벤트를 받아 이 회원이 올린 사진인지 확인한다 (아니면 예외로 롤백)
    @Override
    @Transactional
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        Member member = getMemberUseCase.getById(memberId);
        String nickname = member.getNickname();
        if (request.nickname() != null && !request.nickname().equals(nickname)) {
            if (memberPort.existsByNickname(request.nickname())) {
                throw new CustomException(MemberStatusCode.DUPLICATE_NICKNAME);
            }
            nickname = request.nickname();
        }
        String previousUrl = member.getProfileImageUrl();
        String profileImageUrl = previousUrl;
        if (request.profileImageKey() != null) {
            profileImageUrl = request.profileImageKey().isBlank()
                    ? null
                    : imageUrlResolver.urlOf(imageUrlResolver.permanentKeyOf(request.profileImageKey()));
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
        return getMyProfileUseCase.getMe(memberId);
    }

    @Override
    @Transactional
    public MemberRegisterResult registerIfAbsent(MemberRegisterCommand command) {
        return memberPort.findByProviderAndProviderMemberId(command.provider(), command.providerMemberId())
                .map(member -> {
                    if (member.isWithdrawn()) {
                        throw new CustomException(MemberStatusCode.MEMBER_WITHDRAWN);
                    }
                    return new MemberRegisterResult(member, false);
                })
                .orElseGet(() -> new MemberRegisterResult(memberPort.save(toMember(command)), true));
    }

    // 이메일 회원은 provider = EMAIL, providerMemberId = email 로 저장해
    // (provider, providerMemberId) 유니크로 중복을 막는다. 동시 가입 경합도 유니크 위반으로 걸러진다
    @Override
    @Transactional
    public Member signup(MemberSignupCommand command) {
        if (memberPort.existsByProviderAndProviderMemberId(Provider.EMAIL, command.email())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }
        if (memberPort.existsByNickname(command.nickname())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_NICKNAME);
        }
        try {
            return memberPort.saveAndFlush(Member.builder()
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
    @Transactional
    public Long changePassword(String email, String encodedPassword) {
        Member member = findEmailMemberUseCase.findEmailMember(email)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new CustomException(MemberStatusCode.MEMBER_NOT_FOUND));
        member.changePassword(encodedPassword);
        return member.getId();
    }

    private Member toMember(MemberRegisterCommand command) {
        return Member.builder()
                .provider(command.provider())
                .providerMemberId(command.providerMemberId())
                .email(command.email())
                .nickname(generateNickname())
                .profileImageUrl(command.profileImageUrl())
                .role(Role.USER)
                .build();
    }

    // 소셜 계정의 이름(구글 name 은 실명)은 공개 커뮤니티에 노출되면 안 되므로 쓰지 않고 항상 자동 생성한다.
    // 나중에 PATCH /members/me 로 바꿀 수 있다
    private String generateNickname() {
        for (int i = 0; i < MAX_NICKNAME_TRIES; i++) {
            String candidate = randomNickname();
            if (!memberPort.existsByNickname(candidate)) {
                return candidate;
            }
        }
        // 그래도 겹치면 저장할 때 유니크 제약에 걸려 OAuthService 가 한 번 더 시도한다
        return randomNickname();
    }

    private String randomNickname() {
        // 접두사 5자 + 6자리 = 11자 (닉네임 최대 12자). 4자리(9천 개)로는 회원이 늘면 금방 겹친다
        return DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
