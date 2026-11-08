package ijiri.ijiriserver.user.auth.email.adapter.`in`.scheduler

import ijiri.ijiriserver.user.auth.email.application.port.out.EmailVerificationPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 인증만 하고 가입하지 않았거나, 코드만 받고 방치된 만료 인증 기록 정리.
 */
@Component
class EmailVerificationCleanupScheduler(
    private val emailVerificationPort: EmailVerificationPort,
    private val clock: Clock,
) {

    @Transactional
    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    fun deleteExpired() {
        val deleted = emailVerificationPort.deleteAllExpired(LocalDateTime.now(clock))
        log.info("Deleted {} expired email verifications", deleted)
    }

    companion object {
        private val log = LoggerFactory.getLogger(EmailVerificationCleanupScheduler::class.java)
    }
}
