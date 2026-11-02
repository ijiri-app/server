package ijiri.ijiriserver.global.ratelimit

import ijiri.ijiriserver.global.exception.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class RateLimiterTest {

    @Test
    fun 한도까지는_허용하고_넘으면_거부한다() {
        val rateLimiter = RateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

        assertThat(rateLimiter.tryAcquire("key", 2, PERIOD)).isTrue()
        assertThat(rateLimiter.tryAcquire("key", 2, PERIOD)).isTrue()
        assertThat(rateLimiter.tryAcquire("key", 2, PERIOD)).isFalse()
        assertThatThrownBy { rateLimiter.check("key", 2, PERIOD) }.isInstanceOf(CustomException::class.java)
    }

    @Test
    fun 윈도가_지나면_다시_허용한다() {
        val clock = MutableClock(Instant.EPOCH)
        val rateLimiter = RateLimiter(clock)

        rateLimiter.tryAcquire("key", 1, PERIOD)
        clock.advance(PERIOD)

        assertThat(rateLimiter.tryAcquire("key", 1, PERIOD)).isTrue()
    }

    @Test
    fun 키마다_따로_센다() {
        val rateLimiter = RateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

        rateLimiter.tryAcquire("a", 1, PERIOD)

        assertThat(rateLimiter.tryAcquire("b", 1, PERIOD)).isTrue()
    }

    @Test
    fun 한도_도달_여부는_횟수를_늘리지_않고_확인하고_초기화할_수_있다() {
        val rateLimiter = RateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        rateLimiter.tryAcquire("key", 2, PERIOD)

        assertThat(rateLimiter.isExhausted("key", 2)).isFalse()
        rateLimiter.tryAcquire("key", 2, PERIOD)
        assertThat(rateLimiter.isExhausted("key", 2)).isTrue()

        rateLimiter.reset("key")
        assertThat(rateLimiter.isExhausted("key", 2)).isFalse()
    }

    private class MutableClock(private var now: Instant) : Clock() {

        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun getZone(): ZoneOffset = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    companion object {
        private val PERIOD: Duration = Duration.ofMinutes(1)
    }
}
