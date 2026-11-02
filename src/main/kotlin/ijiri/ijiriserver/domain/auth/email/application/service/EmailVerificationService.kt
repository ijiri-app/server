package ijiri.ijiriserver.domain.auth.email.application.service

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.domain.auth.email.application.port.`in`.ConsumeResetCodeUseCase
import ijiri.ijiriserver.domain.auth.email.application.port.`in`.ConsumeSignupTokenUseCase
import ijiri.ijiriserver.domain.auth.email.application.port.`in`.SendResetCodeUseCase
import ijiri.ijiriserver.domain.auth.email.application.port.`in`.SendSignupCodeUseCase
import ijiri.ijiriserver.domain.auth.email.application.port.`in`.VerifySignupCodeUseCase
import ijiri.ijiriserver.domain.auth.email.application.port.out.EmailVerificationPort
import ijiri.ijiriserver.domain.auth.email.application.port.out.VerificationMailPort
import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken
import ijiri.ijiriserver.domain.member.application.port.`in`.FindEmailMemberUseCase
import ijiri.ijiriserver.global.exception.exception.CustomException
import ijiri.ijiriserver.global.ratelimit.RateLimiter
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.Base64

@Service
class EmailVerificationService(
    private val emailVerificationPort: EmailVerificationPort,
    private val verificationMailPort: VerificationMailPort,
    private val findEmailMemberUseCase: FindEmailMemberUseCase,
    private val rateLimiter: RateLimiter,
    private val clock: Clock,
    private val transactionTemplate: TransactionTemplate,
) : SendSignupCodeUseCase,
    VerifySignupCodeUseCase,
    ConsumeSignupTokenUseCase,
    SendResetCodeUseCase,
    ConsumeResetCodeUseCase {

    // 가입된 이메일도 응답(성공/재발송 대기)은 미가입 이메일과 똑같이 두고 안내 메일만 보내 가입 여부가 드러나지 않게 한다
    override fun sendSignupCode(email: String) {
        val registered = findEmailMemberUseCase.findEmailMember(email)
        if (registered != null) {
            acquireNoticeCooldown(email, VerificationPurpose.SIGNUP)
            if (registered.isWithdrawn()) {
                verificationMailPort.sendWithdrawnAccount(email)
            } else {
                verificationMailPort.sendAlreadyRegistered(email)
            }
            return
        }
        sendCode(email, VerificationPurpose.SIGNUP)
    }

    // 틀린 시도도 횟수가 남아야 하므로 noRollbackFor 로 예외가 나도 attemptCount 증가분은 커밋한다
    @Transactional(noRollbackFor = [CustomException::class])
    override fun verifySignupCode(email: String, code: String): AuthResponse {
        val now = LocalDateTime.now(clock)
        val verification = checkCode(email, VerificationPurpose.SIGNUP, code, now)
        val token = TOKEN_PREFIX + randomToken()
        verification.markVerified(now, RefreshToken.hash(token), now.plusMinutes(TOKEN_VALID_MINUTES))
        return AuthResponse.verified(token)
    }

    @Transactional
    override fun consumeSignupToken(email: String, verificationToken: String) {
        val deleted = emailVerificationPort.deleteVerified(
            email,
            VerificationPurpose.SIGNUP,
            RefreshToken.hash(verificationToken),
            LocalDateTime.now(clock),
        )
        if (deleted == 0) {
            throw CustomException(AuthStatusCode.INVALID_VERIFICATION_TOKEN)
        }
    }

    // 가입 여부가 드러나지 않도록 활성 이메일 회원이 아니어도 같은 응답(재발송 대기 포함)을 주고 메일만 보내지 않는다
    override fun sendResetCode(email: String) {
        val member = findEmailMemberUseCase.findEmailMember(email)
        if (member == null || member.isWithdrawn()) {
            acquireNoticeCooldown(email, VerificationPurpose.RESET_PASSWORD)
            return
        }
        sendCode(email, VerificationPurpose.RESET_PASSWORD)
    }

    @Transactional(noRollbackFor = [CustomException::class])
    override fun consumeResetCode(email: String, code: String) {
        val verification = checkCode(
            email,
            VerificationPurpose.RESET_PASSWORD,
            code,
            LocalDateTime.now(clock),
        )
        emailVerificationPort.delete(verification)
    }

    // 메일 발송은 트랜잭션 밖에서 해 SMTP 응답을 기다리는 동안 DB 커넥션을 잡지 않는다
    private fun sendCode(email: String, purpose: VerificationPurpose) {
        val code = generateCode()
        val saved = transactionTemplate.execute { saveCode(email, purpose, code) }!!
        // 발송에 실패하면 받지 못한 코드가 남아 재발송 대기에 걸리지 않도록 지운다
        try {
            verificationMailPort.sendCode(email, code, CODE_VALID_MINUTES)
        } catch (e: CustomException) {
            emailVerificationPort.delete(saved)
            throw e
        }
    }

    // 이 이메일로 발송된 코드가 없으면 다른 이메일의 코드를 넣은 경우도 포함해 일치하지 않는 것으로 본다.
    // 이미 확인했거나, 만료됐거나, 5번 틀린 코드는 무효
    private fun checkCode(
        email: String,
        purpose: VerificationPurpose,
        code: String,
        now: LocalDateTime,
    ): EmailVerification {
        rateLimiter.check("$ATTEMPT_KEY_PREFIX$purpose:$email", MAX_ATTEMPTS_PER_DAY, ATTEMPT_LIMIT_PERIOD)
        val verification = emailVerificationPort.findByEmailAndPurpose(email, purpose)
            ?: throw invalidCode(0)
        val usable = !verification.isVerified() &&
            !verification.isExpired(now) &&
            !verification.hasExceededAttempts(MAX_ATTEMPTS)
        if (!usable) {
            throw invalidCode(0)
        }
        if (!verification.matches(code)) {
            throw invalidCode(verification.remainingAttempts(MAX_ATTEMPTS))
        }
        return verification
    }

    // 코드를 발송하지 않는 경우에도 실제 발송과 같은 재발송 대기·같은 에러를 적용한다
    private fun acquireNoticeCooldown(email: String, purpose: VerificationPurpose) {
        val key = "$NOTICE_KEY_PREFIX$purpose:$email"
        if (!rateLimiter.tryAcquire(key, 1, Duration.ofSeconds(RESEND_COOLDOWN_SECONDS))) {
            throw resendTooSoon(rateLimiter.retryAfterSeconds(key))
        }
    }

    // 기존 행은 잠근 뒤 재발송 대기를 확인하고 갱신한다.
    // 같은 이메일로 처음 발송 요청이 동시에 들어오면 잠글 행이 없어 한쪽은 유니크 제약에 걸린다
    private fun saveCode(email: String, purpose: VerificationPurpose, code: String): EmailVerification {
        val now = LocalDateTime.now(clock)
        val expiresAt = now.plusMinutes(CODE_VALID_MINUTES)
        val existing = emailVerificationPort.findByEmailAndPurposeForUpdate(
            email,
            purpose,
        )
        if (existing != null) {
            if (!existing.canResend(now, RESEND_COOLDOWN_SECONDS)) {
                throw resendTooSoon(existing.secondsUntilResend(now, RESEND_COOLDOWN_SECONDS))
            }
            existing.resend(code, now, expiresAt)
            return existing
        }
        try {
            return emailVerificationPort.saveAndFlush(
                EmailVerification.of(email, purpose, code, now, expiresAt)
            )
        } catch (e: DataIntegrityViolationException) {
            throw resendTooSoon(RESEND_COOLDOWN_SECONDS)
        }
    }

    private fun invalidCode(remainingAttempts: Int): CustomException =
        CustomException(
            AuthStatusCode.INVALID_VERIFICATION_CODE,
            mapOf("remainingAttempts" to remainingAttempts),
        )

    private fun resendTooSoon(retryAfterSeconds: Long): CustomException =
        CustomException(
            AuthStatusCode.VERIFICATION_RESEND_TOO_SOON,
            mapOf("retryAfterSeconds" to retryAfterSeconds),
        )

    private fun generateCode(): String =
        buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) {
                append(CODE_CHARACTERS[RANDOM.nextInt(CODE_CHARACTERS.length)])
            }
        }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        RANDOM.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val CODE_VALID_MINUTES = 5L
        // 가입 코드 확인 후 가입까지 허용하는 시간 = verificationToken 유효 시간
        private const val TOKEN_VALID_MINUTES = 10L
        private const val RESEND_COOLDOWN_SECONDS = 60L
        private const val MAX_ATTEMPTS = 5
        private const val CODE_LENGTH = 6
        private const val CODE_CHARACTERS = "0123456789"
        private const val TOKEN_PREFIX = "vt_"
        private val RANDOM = SecureRandom()
        private const val NOTICE_KEY_PREFIX = "verification:notice:"
        // 코드당 5번 + 1분마다 재발송이면 IP 를 바꿔 가며 6자리 코드를 계속 맞혀 볼 수 있으므로 이메일별 하루 시도 수를 묶는다
        private const val ATTEMPT_KEY_PREFIX = "verification:attempt:"
        private const val MAX_ATTEMPTS_PER_DAY = 10
        private val ATTEMPT_LIMIT_PERIOD = Duration.ofDays(1)
    }
}
