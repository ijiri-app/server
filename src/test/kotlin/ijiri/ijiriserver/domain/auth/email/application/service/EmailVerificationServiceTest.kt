package ijiri.ijiriserver.domain.auth.email.application.service

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.domain.auth.email.application.port.out.EmailVerificationPort
import ijiri.ijiriserver.domain.auth.email.application.port.out.VerificationMailPort
import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose
import ijiri.ijiriserver.domain.member.application.port.`in`.FindEmailMemberUseCase
import ijiri.ijiriserver.domain.member.entity.Member
import ijiri.ijiriserver.domain.member.entity.Provider
import ijiri.ijiriserver.domain.member.entity.Role
import ijiri.ijiriserver.global.exception.exception.CustomException
import ijiri.ijiriserver.global.ratelimit.RateLimiter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 메일은 응답 뒤에 실행기로 보낸다. 실행기를 큐로 두고 직접 돌려, 응답 시점에는 어떤 메일도 나가지 않았음을 확인한다.
 */
class EmailVerificationServiceTest {

    private val emailVerificationPort: EmailVerificationPort = mock()
    private val verificationMailPort: VerificationMailPort = mock()
    private val findEmailMemberUseCase: FindEmailMemberUseCase = mock()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val queued = mutableListOf<Runnable>()
    private val transactionManager: PlatformTransactionManager = mock {
        on { getTransaction(any()) } doReturn SimpleTransactionStatus()
    }
    private val service = EmailVerificationService(
        emailVerificationPort,
        verificationMailPort,
        findEmailMemberUseCase,
        RateLimiter(clock),
        clock,
        TransactionTemplate(transactionManager),
        TaskExecutor { task -> queued += task },
    )

    @Test
    fun 코드_메일은_행을_저장한_뒤_실행기에서_보낸다() {
        whenever(emailVerificationPort.saveAndFlush(any())).thenAnswer { it.getArgument<EmailVerification>(0) }

        service.sendSignupCode(EMAIL)

        verify(emailVerificationPort).saveAndFlush(any())
        verify(verificationMailPort, never()).sendCode(any(), any(), any())
        queued.single().run()
        verify(verificationMailPort).sendCode(eq(EMAIL), any(), eq(5L))
    }

    @Test
    fun 가입된_이메일도_코드_행을_만들고_안내_메일만_보낸다() {
        whenever(findEmailMemberUseCase.findEmailMember(EMAIL)).thenReturn(emailMember())
        whenever(emailVerificationPort.saveAndFlush(any())).thenAnswer { it.getArgument<EmailVerification>(0) }

        service.sendSignupCode(EMAIL)
        queued.single().run()

        verify(emailVerificationPort).saveAndFlush(any())
        verify(verificationMailPort).sendAlreadyRegistered(EMAIL)
        verify(verificationMailPort, never()).sendCode(any(), any(), any())
    }

    @Test
    fun 회원이_아닌_이메일의_재설정_코드는_행만_만들고_메일을_보내지_않는다() {
        whenever(emailVerificationPort.saveAndFlush(any())).thenAnswer { it.getArgument<EmailVerification>(0) }

        service.sendResetCode(EMAIL)
        queued.single().run()

        verify(emailVerificationPort).saveAndFlush(any())
        verify(verificationMailPort, never()).sendCode(any(), any(), any())
    }

    @Test
    fun 발송에_실패하면_응답은_바꾸지_않고_코드_행을_지운다() {
        val now = LocalDateTime.now(clock)
        val saved = EmailVerification.of(EMAIL, VerificationPurpose.SIGNUP, "123456", now, now.plusMinutes(5))
        whenever(emailVerificationPort.saveAndFlush(any())).thenReturn(saved)
        whenever(verificationMailPort.sendCode(any(), any(), any()))
            .thenThrow(CustomException(AuthStatusCode.EMAIL_SEND_FAILED))

        service.sendSignupCode(EMAIL)
        queued.single().run()

        verify(emailVerificationPort).delete(saved)
    }

    private fun emailMember(): Member =
        Member(
            nickname = "이지리",
            provider = Provider.EMAIL,
            providerMemberId = EMAIL,
            role = Role.USER,
            email = EMAIL,
            password = "hash",
        )

    companion object {
        private const val EMAIL = "user@ijiri.com"
    }
}
