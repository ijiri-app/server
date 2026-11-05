package ijiri.ijiriserver.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

/**
 * 모든 LocalDateTime 을 서버 JVM 타임존이 아닌 Asia/Seoul 기준으로 맞춘다 (스케줄러 zone 과 동일).
 * 생성·수정 시각 감사와 스케줄러도 모든 서비스가 쓰므로 여기서 켠다.
 */
@Configuration
@EnableScheduling
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.system(ZONE)

    @Bean
    fun auditingDateTimeProvider(clock: Clock): DateTimeProvider =
        DateTimeProvider { Optional.of(LocalDateTime.now(clock)) }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
