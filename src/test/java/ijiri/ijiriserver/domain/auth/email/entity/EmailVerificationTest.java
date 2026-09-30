package ijiri.ijiriserver.domain.auth.email.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void 코드는_대소문자_구분없이_비교하고_시도마다_횟수가_오른다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "A1B2C3", NOW, NOW.plusMinutes(10));

        assertThat(verification.matches("zzzzzz")).isFalse();
        assertThat(verification.matches("a1b2c3")).isTrue();
        assertThat(verification.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void 시도_횟수_초과를_판단한다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "A1B2C3", NOW, NOW.plusMinutes(10));
        for (int i = 0; i < 5; i++) {
            verification.matches("zzzzzz");
        }

        assertThat(verification.hasExceededAttempts(5)).isTrue();
    }

    @Test
    void 재발송_대기와_만료를_판단하고_재발송하면_시도_횟수를_초기화한다() {
        EmailVerification verification = EmailVerification.of("a@b.com", "A1B2C3", NOW, NOW.plusMinutes(10));
        verification.matches("zzzzzz");

        assertThat(verification.canResend(NOW.plusSeconds(59), 60)).isFalse();
        assertThat(verification.canResend(NOW.plusSeconds(60), 60)).isTrue();
        assertThat(verification.isExpired(NOW.plusMinutes(11))).isTrue();

        verification.resend("D4E5F6", NOW.plusMinutes(1), NOW.plusMinutes(11));
        assertThat(verification.getAttemptCount()).isZero();
        assertThat(verification.isExpired(NOW.plusMinutes(10))).isFalse();
    }
}
