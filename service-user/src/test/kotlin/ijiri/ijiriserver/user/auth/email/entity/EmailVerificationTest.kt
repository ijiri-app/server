package ijiri.ijiriserver.user.auth.email.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class EmailVerificationTest {

    @Test
    fun 코드를_비교하고_시도마다_횟수가_오른다() {
        val verification = newVerification()

        assertThat(verification.matches("000000")).isFalse()
        assertThat(verification.matches("123456")).isTrue()
        assertThat(verification.attemptCount).isEqualTo(2)
    }

    @Test
    fun 시도_횟수_초과를_판단한다() {
        val verification = newVerification()
        repeat(5) {
            verification.matches("000000")
        }

        assertThat(verification.hasExceededAttempts(5)).isTrue()
    }

    @Test
    fun 인증_완료되면_만료_시각이_verificationToken_기한으로_바뀐다() {
        val verification = newVerification()

        verification.markVerified(NOW.plusMinutes(1), "token-hash", NOW.plusMinutes(11))

        assertThat(verification.isVerified()).isTrue()
        assertThat(verification.isExpired(NOW.plusMinutes(10))).isFalse()
    }

    @Test
    fun 재발송하면_인증_상태와_시도_횟수를_초기화한다() {
        val verification = newVerification()
        verification.matches("000000")
        verification.markVerified(NOW, "token-hash", NOW.plusMinutes(10))

        assertThat(verification.canResend(NOW.plusSeconds(59), 60)).isFalse()
        assertThat(verification.canResend(NOW.plusSeconds(60), 60)).isTrue()

        verification.resend("654321", NOW.plusMinutes(1), NOW.plusMinutes(6))
        assertThat(verification.attemptCount).isZero()
        assertThat(verification.isVerified()).isFalse()
        assertThat(verification.isExpired(NOW.plusMinutes(7))).isTrue()
    }

    private fun newVerification(): EmailVerification =
        EmailVerification.of("a@b.com", VerificationPurpose.SIGNUP, "123456", NOW, NOW.plusMinutes(5))

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 1, 1, 12, 0)
    }
}
