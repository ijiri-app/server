package ijiri.ijiriserver.global.ratelimit;

import ijiri.ijiriserver.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private static final Duration PERIOD = Duration.ofMinutes(1);

    @Test
    void 한도까지는_허용하고_넘으면_거부한다() {
        RateLimiter rateLimiter = new RateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThat(rateLimiter.tryAcquire("key", 2, PERIOD)).isTrue();
        assertThat(rateLimiter.tryAcquire("key", 2, PERIOD)).isTrue();
        assertThat(rateLimiter.tryAcquire("key", 2, PERIOD)).isFalse();
        assertThatThrownBy(() -> rateLimiter.check("key", 2, PERIOD)).isInstanceOf(CustomException.class);
    }

    @Test
    void 윈도가_지나면_다시_허용한다() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        RateLimiter rateLimiter = new RateLimiter(clock);

        rateLimiter.tryAcquire("key", 1, PERIOD);
        clock.advance(PERIOD);

        assertThat(rateLimiter.tryAcquire("key", 1, PERIOD)).isTrue();
    }

    @Test
    void 키마다_따로_센다() {
        RateLimiter rateLimiter = new RateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        rateLimiter.tryAcquire("a", 1, PERIOD);

        assertThat(rateLimiter.tryAcquire("b", 1, PERIOD)).isTrue();
    }

    @Test
    void 한도_도달_여부는_횟수를_늘리지_않고_확인하고_초기화할_수_있다() {
        RateLimiter rateLimiter = new RateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        rateLimiter.tryAcquire("key", 2, PERIOD);

        assertThat(rateLimiter.isExhausted("key", 2)).isFalse();
        rateLimiter.tryAcquire("key", 2, PERIOD);
        assertThat(rateLimiter.isExhausted("key", 2)).isTrue();

        rateLimiter.reset("key");
        assertThat(rateLimiter.isExhausted("key", 2)).isFalse();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
