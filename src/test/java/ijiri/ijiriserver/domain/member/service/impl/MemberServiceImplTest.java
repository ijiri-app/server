package ijiri.ijiriserver.domain.member.service.impl;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import ijiri.ijiriserver.domain.member.service.MemberPostCounter;
import ijiri.ijiriserver.domain.member.service.MemberWishCounter;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.storage.ImageUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-31T00:00:00Z");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final MemberServiceImpl memberService = new MemberServiceImpl(
            memberRepository,
            mock(MemberPostCounter.class),
            mock(MemberWishCounter.class),
            new ImageUrlResolver("http://localhost"),
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void 탈퇴하면_행을_지우지_않고_탈퇴_시각을_기록한_뒤_즉시_처리_이벤트를_발행한다() {
        Member member = kakaoMember(null);
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        memberService.withdraw(1L);

        assertThat(member.getDeletedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        verify(memberRepository, never()).delete(any());
        verify(eventPublisher).publishEvent(new MemberWithdrawnEvent(1L, Provider.KAKAO, "kakao-1"));
    }

    @Test
    void 보관_기간_중인_탈퇴_계정으로는_소셜_로그인할_수_없다() {
        when(memberRepository.findByProviderAndProviderMemberId(Provider.KAKAO, "kakao-1"))
                .thenReturn(Optional.of(kakaoMember(LocalDateTime.of(2026, 1, 20, 0, 0))));
        MemberRegisterCommand command = new MemberRegisterCommand(Provider.KAKAO, "kakao-1", null, null, null);

        assertThatThrownBy(() -> memberService.registerIfAbsent(command)).isInstanceOf(CustomException.class);
    }

    @Test
    void 영구_삭제는_데이터_정리_이벤트를_먼저_발행하고_행을_지운다() {
        Member member = kakaoMember(LocalDateTime.of(2025, 12, 1, 0, 0));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.purge(1L);

        // 보관 기간(30일) + 재시도 기간(3일)이 지났으므로 마지막 시도
        verify(eventPublisher).publishEvent(new MemberPurgedEvent(1L, Provider.KAKAO, "kakao-1", true));
        verify(memberRepository).delete(member);
    }

    @Test
    void 탈퇴하지_않은_회원은_영구_삭제하지_않는다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(kakaoMember(null)));

        memberService.purge(1L);

        verify(memberRepository, never()).delete(any());
    }

    @Test
    void 소셜_가입은_제공자_이름을_쓰지_않고_닉네임을_자동_생성한다() {
        when(memberRepository.findByProviderAndProviderMemberId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemberRegisterResult result = memberService.registerIfAbsent(
                new MemberRegisterCommand(Provider.GOOGLE, "google-1", null, "홍길동", null)
        );

        assertThat(result.member().getNickname()).matches("이지리오너\\d{6}");
    }

    private Member kakaoMember(LocalDateTime deletedAt) {
        return Member.builder()
                .id(1L)
                .provider(Provider.KAKAO)
                .providerMemberId("kakao-1")
                .nickname("이지리")
                .role(Role.USER)
                .deletedAt(deletedAt)
                .build();
    }
}
