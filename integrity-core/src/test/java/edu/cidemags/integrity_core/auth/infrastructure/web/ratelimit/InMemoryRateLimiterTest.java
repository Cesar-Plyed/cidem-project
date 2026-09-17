package edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private InMemoryRateLimiter limiterAt(Instant instant, int maxAttempts, long windowSeconds) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        return new InMemoryRateLimiter(maxAttempts, windowSeconds, clock);
    }

    @Test
    void should_allow_requests_up_to_the_max_attempts_within_the_window() {
        InMemoryRateLimiter limiter = limiterAt(Instant.parse("2026-01-01T00:00:00Z"), 3, 60);

        assertThat(limiter.tryConsume("same-key")).isTrue();
        assertThat(limiter.tryConsume("same-key")).isTrue();
        assertThat(limiter.tryConsume("same-key")).isTrue();
    }

    @Test
    void should_reject_the_request_that_exceeds_the_max_attempts() {
        InMemoryRateLimiter limiter = limiterAt(Instant.parse("2026-01-01T00:00:00Z"), 3, 60);

        limiter.tryConsume("same-key");
        limiter.tryConsume("same-key");
        limiter.tryConsume("same-key");

        assertThat(limiter.tryConsume("same-key")).isFalse();
    }

    @Test
    void different_keys_should_have_independent_counters() {
        InMemoryRateLimiter limiter = limiterAt(Instant.parse("2026-01-01T00:00:00Z"), 1, 60);

        assertThat(limiter.tryConsume("ip-1:/auth/login")).isTrue();
        assertThat(limiter.tryConsume("ip-2:/auth/login")).isTrue();
        assertThat(limiter.tryConsume("ip-1:/auth/register")).isTrue();
    }

    @Test
    void should_reset_the_counter_once_the_window_has_fully_elapsed() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        InMemoryRateLimiter limiterNow = limiterAt(start, 1, 60);

        assertThat(limiterNow.tryConsume("same-key")).isTrue();
        assertThat(limiterNow.tryConsume("same-key")).isFalse();
    }
}