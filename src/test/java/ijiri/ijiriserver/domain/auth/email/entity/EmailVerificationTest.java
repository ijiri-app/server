package ijiri.ijiriserver.domain.auth.email.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void 코드를_비교하고_시도마다_횟수가_오른다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "123456", NOW, NOW.plusMinutes(5));

        assertThat(verification.matches("000000")).isFalse();
        assertThat(verification.matches("123456")).isTrue();
        assertThat(verification.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void 시도_횟수_초과를_판단한다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "123456", NOW, NOW.plusMinutes(5));
        for (int i = 0; i < 5; i++) {
            verification.matches("000000");
        }

        assertThat(verification.hasExceededAttempts(5)).isTrue();
    }

    @Test
    void 인증_완료되면_만료_시각이_가입_기한으로_늘어난다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "123456", NOW, NOW.plusMinutes(5));

        verification.markVerified(NOW.plusMinutes(1), NOW.plusMinutes(31));

        assertThat(verification.isVerified()).isTrue();
        assertThat(verification.isExpired(NOW.plusMinutes(10))).isFalse();
    }

    @Test
    void 재발송하면_인증_상태와_시도_횟수를_초기화한다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "123456", NOW, NOW.plusMinutes(5));
        verification.matches("000000");
        verification.markVerified(NOW, NOW.plusMinutes(30));

        assertThat(verification.canResend(NOW.plusSeconds(59), 60)).isFalse();
        assertThat(verification.canResend(NOW.plusSeconds(60), 60)).isTrue();

        verification.resend("654321", NOW.plusMinutes(1), NOW.plusMinutes(6));
        assertThat(verification.getAttemptCount()).isZero();
        assertThat(verification.isVerified()).isFalse();
        assertThat(verification.isExpired(NOW.plusMinutes(7))).isTrue();
    }
}
