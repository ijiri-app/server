package ijiri.ijiriserver.domain.auth.email.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 이메일 + 용도(가입, 비밀번호 재설정)당 한 줄. 코드 재발송 시 같은 행을 갱신한다.
 * 코드가 맞으면 verificationToken 을 발급해 해시만 저장하고, 만료 시각을 토큰 유효 기한으로 바꾼다.
 * 가입(재설정)에 성공하면 삭제해 한 번의 인증을 한 번만 쓸 수 있게 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "email_verification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_email_verification_email_purpose",
                columnNames = {"email", "purpose"}
        )
)
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20, updatable = false)
    private VerificationPurpose purpose;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    // 코드 확인 전에는 코드 만료 시각, 확인 후에는 verificationToken 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    // null 이면 아직 코드를 확인하지 않은 상태
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 원문은 응답으로만 내보내고 SHA-256 해시만 저장한다
    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    public static EmailVerification of(
            String email,
            VerificationPurpose purpose,
            String code,
            LocalDateTime now,
            LocalDateTime expiresAt
    ) {
        return EmailVerification.builder()
                .email(email)
                .purpose(purpose)
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
        this.verifiedAt = null;
        this.verificationTokenHash = null;
    }

    public void markVerified(LocalDateTime now, String verificationTokenHash, LocalDateTime tokenExpiresAt) {
        this.verifiedAt = now;
        this.verificationTokenHash = verificationTokenHash;
        this.expiresAt = tokenExpiresAt;
    }

    public long secondsUntilResend(LocalDateTime now, long cooldownSeconds) {
        return Math.max(1, Duration.between(now, sentAt.plusSeconds(cooldownSeconds)).toSeconds());
    }

    public int remainingAttempts(int maxAttempts) {
        return Math.max(0, maxAttempts - attemptCount);
    }

    public boolean isVerified() {
        return verifiedAt != null;
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
        return this.code.equals(code);
    }
}
