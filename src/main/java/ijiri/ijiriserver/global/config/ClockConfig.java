package ijiri.ijiriserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 모든 LocalDateTime 을 서버 JVM 타임존이 아닌 Asia/Seoul 기준으로 맞춘다 (스케줄러 zone 과 동일).
 */
@Configuration
public class ClockConfig {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
