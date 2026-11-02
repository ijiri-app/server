package ijiri.ijiriserver.user.auth.token.adapter.`in`.scheduler

import ijiri.ijiriserver.user.auth.token.application.port.out.RefreshTokenPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 로그아웃/재발급 없이 방치된(앱 삭제, 장기 미접속) 만료 refresh token 정리.
 */
@Component
class RefreshTokenCleanupScheduler(
    private val refreshTokenPort: RefreshTokenPort,
    private val clock: Clock,
) {

    @Transactional
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun deleteExpiredTokens() {
        val deleted = refreshTokenPort.deleteAllExpired(LocalDateTime.now(clock))
        log.info("Deleted {} expired refresh tokens", deleted)
    }

    companion object {
        private val log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler::class.java)
    }
}
