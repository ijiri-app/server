package ijiri.ijiriserver.global.ratelimit;

import ijiri.ijiriserver.global.exception.CommonStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 키별 고정 윈도 요청 횟수 제한. 단일 인스턴스 메모리 기반이라 서버를 여러 대 띄우면 인스턴스별로 따로 센다.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public void check(String key, int limit, Duration period) {
        if (!tryAcquire(key, limit, period)) {
            throw new CustomException(CommonStatusCode.TOO_MANY_REQUESTS);
        }
    }

    public boolean tryAcquire(String key, int limit, Duration period) {
        Instant now = clock.instant();
        Window window = windows.compute(
                key,
                (k, current) -> current == null || current.isExpired(now)
                        ? new Window(now.plus(period), 1)
                        : current.increment()
        );
        return window.count() <= limit;
    }

    @Scheduled(fixedRate = 600_000)
    public void purgeExpired() {
        Instant now = clock.instant();
        windows.values().removeIf(window -> window.isExpired(now));
    }

    private record Window(Instant expiresAt, int count) {

        boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }

        Window increment() {
            return new Window(expiresAt, count + 1);
        }
    }
}
