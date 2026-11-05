package ijiri.ijiriserver.common.ratelimit

import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.exception.CommonStatusCode
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 키별 고정 윈도 요청 횟수 제한. 단일 인스턴스 메모리 기반이라 서버를 여러 대 띄우면 인스턴스별로 따로 센다.
 */
@Component
class RateLimiter(
    private val clock: Clock,
) {

    private val windows: MutableMap<String, Window> = ConcurrentHashMap()

    fun check(key: String, limit: Int, period: Duration) {
        if (!tryAcquire(key, limit, period)) {
            throw CustomException(
                CommonStatusCode.TOO_MANY_REQUESTS,
                mapOf("retryAfterSeconds" to retryAfterSeconds(key)),
            )
        }
    }

    fun tryAcquire(key: String, limit: Int, period: Duration): Boolean {
        val now = clock.instant()
        val window = windows.compute(key) { _, current ->
            if (current == null || current.isExpired(now)) Window(now.plus(period), 1) else current.increment()
        }!!
        return window.count <= limit
    }

    // 횟수를 늘리지 않고 이미 한도에 도달했는지만 본다 (실패 횟수 기반 잠금 확인용)
    fun isExhausted(key: String, limit: Int): Boolean {
        val window = windows[key]
        return window != null && !window.isExpired(clock.instant()) && window.count >= limit
    }

    // 현재 윈도가 끝날 때까지 남은 초 (최소 1). 윈도가 없으면 0
    fun retryAfterSeconds(key: String): Long {
        val window = windows[key] ?: return 0
        val millis = Duration.between(clock.instant(), window.expiresAt).toMillis()
        return maxOf(1, TimeUnit.MILLISECONDS.toSeconds(millis + 999))
    }

    fun reset(key: String) {
        windows.remove(key)
    }

    @Scheduled(fixedRate = 600_000)
    fun purgeExpired() {
        val now = clock.instant()
        windows.values.removeIf { it.isExpired(now) }
    }

    private data class Window(val expiresAt: Instant, val count: Int) {

        fun isExpired(now: Instant): Boolean = !expiresAt.isAfter(now)

        fun increment(): Window = Window(expiresAt, count + 1)
    }
}
