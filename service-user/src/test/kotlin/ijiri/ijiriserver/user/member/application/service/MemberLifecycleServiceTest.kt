package ijiri.ijiriserver.user.member.application.service

import ijiri.ijiriserver.user.member.application.data.MemberRegisterCommand
import ijiri.ijiriserver.user.member.application.data.MemberSignupCommand
import ijiri.ijiriserver.user.member.application.port.out.MemberCountReader
import ijiri.ijiriserver.user.member.application.port.out.MemberEventPublisher
import ijiri.ijiriserver.user.member.application.port.out.MemberPort
import ijiri.ijiriserver.user.member.application.port.out.ProfileImagePort
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.user.member.entity.Role
import ijiri.ijiriserver.user.member.event.MemberPurgedEvent
import ijiri.ijiriserver.user.member.event.MemberWithdrawnEvent
import ijiri.ijiriserver.user.member.exception.MemberStatusCode
import ijiri.ijiriserver.common.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class MemberLifecycleServiceTest {

    private val memberPort: MemberPort = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()
    private val memberEventPublisher: MemberEventPublisher = mock()
    private val memberQueryService = MemberQueryService(memberPort, mock<MemberCountReader>())
    private val memberCommandService = MemberCommandService(
        memberPort,
        mock<ProfileImagePort>(),
        memberQueryService,
        memberQueryService,
        memberQueryService,
    )
    private val memberLifecycleService = MemberLifecycleService(
        memberPort,
        eventPublisher,
        memberEventPublisher,
        Clock.fixed(NOW, ZoneOffset.UTC),
        memberQueryService,
        memberQueryService,
    )

    @Test
    fun 탈퇴하면_행을_지우지_않고_탈퇴_시각을_기록한_뒤_즉시_처리_이벤트를_발행한다() {
        val member = kakaoMember(null)
        whenever(memberPort.findByIdAndDeletedAtIsNull(1L)).thenReturn(member)

        memberLifecycleService.withdraw(1L)

        assertThat(member.deletedAt).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))
        verify(memberPort, never()).delete(any())
        verify(eventPublisher).publishEvent(MemberWithdrawnEvent(1L, Provider.KAKAO, "kakao-1"))
        verify(memberEventPublisher).withdrawn(1L)
    }

    @Test
    fun 보관_기간_중인_탈퇴_계정으로는_소셜_로그인할_수_없다() {
        whenever(memberPort.findByProviderAndProviderMemberId(Provider.KAKAO, "kakao-1"))
            .thenReturn(kakaoMember(LocalDateTime.of(2026, 1, 20, 0, 0)))
        val command = MemberRegisterCommand(Provider.KAKAO, "kakao-1", null, null, null)

        assertThatThrownBy { memberCommandService.registerIfAbsent(command) }
            .isInstanceOf(CustomException::class.java)
    }

    @Test
    fun 영구_삭제는_데이터_정리_이벤트를_먼저_발행하고_행을_지운다() {
        val member = kakaoMember(LocalDateTime.of(2025, 12, 1, 0, 0))
        whenever(memberPort.findById(1L)).thenReturn(member)

        memberLifecycleService.purge(1L)

        // 보관 기간(30일) + 재시도 기간(3일)이 지났으므로 마지막 시도
        verify(eventPublisher).publishEvent(MemberPurgedEvent(1L, Provider.KAKAO, "kakao-1", true))
        verify(memberEventPublisher).purged(1L)
        verify(memberPort).delete(member)
    }

    @Test
    fun 탈퇴하지_않은_회원은_영구_삭제하지_않는다() {
        whenever(memberPort.findById(1L)).thenReturn(kakaoMember(null))

        memberLifecycleService.purge(1L)

        verify(memberPort, never()).delete(any())
        verify(memberEventPublisher, never()).purged(any())
    }

    @Test
    fun 소셜_가입은_제공자_이름을_쓰지_않고_닉네임을_자동_생성한다() {
        whenever(memberPort.findByProviderAndProviderMemberId(any(), any())).thenReturn(null)
        whenever(memberPort.save(any())).thenAnswer { invocation -> invocation.getArgument<Member>(0) }

        val result = memberCommandService.registerIfAbsent(
            MemberRegisterCommand(Provider.GOOGLE, "google-1", null, "홍길동", null),
        )

        assertThat(result.member.nickname).matches("이지리오너\\d{6}")
    }

    @Test
    fun 동시_가입_경합에서_닉네임_제약에_걸리면_닉네임_중복으로_응답한다() {
        whenever(memberPort.saveAndFlush(any())).thenThrow(
            DataIntegrityViolationException(
                "duplicate",
                ConstraintViolationException("duplicate", SQLException(), Member.NICKNAME_UNIQUE_CONSTRAINT),
            ),
        )

        assertThatThrownBy { memberCommandService.signup(MemberSignupCommand("a@ijiri.com", "이지리", "encoded")) }
            .isInstanceOfSatisfying(CustomException::class.java) { e ->
                assertThat(e.statusCode).isEqualTo(MemberStatusCode.DUPLICATE_NICKNAME)
            }
    }

    private fun kakaoMember(deletedAt: LocalDateTime?): Member =
        Member(
            id = 1L,
            provider = Provider.KAKAO,
            providerMemberId = "kakao-1",
            nickname = "이지리",
            role = Role.USER,
            deletedAt = deletedAt,
        )

    companion object {
        private val NOW: Instant = Instant.parse("2026-01-31T00:00:00Z")
    }
}
