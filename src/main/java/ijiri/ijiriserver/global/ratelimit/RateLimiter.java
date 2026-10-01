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
import java.util.concurrent.TimeUnit;
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
            throw new CustomException(
                    CommonStatusCode.TOO_MANY_REQUESTS,
                    Map.of("retryAfterSeconds", retryAfterSeconds(key))
            );
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

    // 횟수를 늘리지 않고 이미 한도에 도달했는지만 본다 (실패 횟수 기반 잠금 확인용)
    public boolean isExhausted(String key, int limit) {
        Window window = windows.get(key);
        return window != null && !window.isExpired(clock.instant()) && window.count() >= limit;
    }

    // 현재 윈도가 끝날 때까지 남은 초 (최소 1). 윈도가 없으면 0
    public long retryAfterSeconds(String key) {
        Window window = windows.get(key);
        if (window == null) {
            return 0;
        }
        long millis = Duration.between(clock.instant(), window.expiresAt()).toMillis();
        return Math.max(1, TimeUnit.MILLISECONDS.toSeconds(millis + 999));
    }

    public void reset(String key) {
        windows.remove(key);
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
