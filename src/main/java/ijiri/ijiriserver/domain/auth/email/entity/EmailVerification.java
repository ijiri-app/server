package ijiri.ijiriserver.domain.auth.email.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이메일당 한 줄. 코드 재발송 시 같은 행을 갱신하고,
 * 가입에 성공하면 즉시 삭제해 코드를 한 번만 쓸 수 있게 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "email_verification")
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, updatable = false)
    private String email;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    public static EmailVerification of(String email, String code, LocalDateTime now, LocalDateTime expiresAt) {
        return EmailVerification.builder()
                .email(email)
                .code(code)
                .sentAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    public void resend(String code, LocalDateTime now, LocalDateTime expiresAt) {
        this.code = code;
        this.sentAt = now;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }

    public boolean canResend(LocalDateTime now, long cooldownSeconds) {
        return !sentAt.plusSeconds(cooldownSeconds).isAfter(now);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean hasExceededAttempts(int maxAttempts) {
        return attemptCount >= maxAttempts;
    }

    // 틀린 시도도 횟수에 포함되도록 비교 전에 증가시킨다
    public boolean matches(String code) {
        attemptCount++;
        return this.code.equalsIgnoreCase(code);
    }
}
