package ijiri.ijiriserver.user.auth.email.entity

import ijiri.ijiriserver.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Duration
import java.time.LocalDateTime

/**
 * 이메일 + 용도(가입, 비밀번호 재설정)당 한 줄. 코드 재발송 시 같은 행을 갱신한다.
 * 코드가 맞으면 verificationToken 을 발급해 해시만 저장하고, 만료 시각을 토큰 유효 기한으로 바꾼다.
 * 가입(재설정)에 성공하면 삭제해 한 번의 인증을 한 번만 쓸 수 있게 한다.
 */
@Entity
@Table(
    name = "email_verification",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_email_verification_email_purpose",
            columnNames = ["email", "purpose"],
        ),
    ],
)
class EmailVerification(
    email: String,
    purpose: VerificationPurpose,
    code: String,
    sentAt: LocalDateTime,
    expiresAt: LocalDateTime,
    attemptCount: Int = 0,
    verifiedAt: LocalDateTime? = null,
    verificationTokenHash: String? = null,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "email", nullable = false, updatable = false)
    var email: String = email
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20, updatable = false)
    var purpose: VerificationPurpose = purpose
        protected set

    @Column(name = "code", nullable = false, length = 6)
    var code: String = code
        protected set

    @Column(name = "sent_at", nullable = false)
    var sentAt: LocalDateTime = sentAt
        protected set

    // 코드 확인 전에는 코드 만료 시각, 확인 후에는 verificationToken 만료 시각
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = expiresAt
        protected set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = attemptCount
        protected set

    // null 이면 아직 코드를 확인하지 않은 상태
    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = verifiedAt
        protected set

    // 원문은 응답으로만 내보내고 SHA-256 해시만 저장한다
    @Column(name = "verification_token_hash", length = 64)
    var verificationTokenHash: String? = verificationTokenHash
        protected set

    fun resend(code: String, now: LocalDateTime, expiresAt: LocalDateTime) {
        this.code = code
        this.sentAt = now
        this.expiresAt = expiresAt
        this.attemptCount = 0
        this.verifiedAt = null
        this.verificationTokenHash = null
    }

    fun markVerified(now: LocalDateTime, verificationTokenHash: String, tokenExpiresAt: LocalDateTime) {
        this.verifiedAt = now
        this.verificationTokenHash = verificationTokenHash
        this.expiresAt = tokenExpiresAt
    }

    fun secondsUntilResend(now: LocalDateTime, cooldownSeconds: Long): Long =
        maxOf(1L, Duration.between(now, sentAt.plusSeconds(cooldownSeconds)).toSeconds())

    fun remainingAttempts(maxAttempts: Int): Int = maxOf(0, maxAttempts - attemptCount)

    fun isVerified(): Boolean = verifiedAt != null

    fun canResend(now: LocalDateTime, cooldownSeconds: Long): Boolean =
        !sentAt.plusSeconds(cooldownSeconds).isAfter(now)

    fun isExpired(now: LocalDateTime): Boolean = expiresAt.isBefore(now)

    fun hasExceededAttempts(maxAttempts: Int): Boolean = attemptCount >= maxAttempts

    // 틀린 시도도 횟수에 포함되도록 비교 전에 증가시킨다
    fun matches(code: String): Boolean {
        attemptCount++
        return this.code == code
    }

    companion object {
        fun of(
            email: String,
            purpose: VerificationPurpose,
            code: String,
            now: LocalDateTime,
            expiresAt: LocalDateTime,
        ): EmailVerification = EmailVerification(
            email = email,
            purpose = purpose,
            code = code,
            sentAt = now,
            expiresAt = expiresAt,
        )
    }
}
