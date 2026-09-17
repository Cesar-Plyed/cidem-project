package edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> attemptsByKey = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(
            @Value("${security.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.rate-limit.window-seconds:60}") long windowSeconds,
            Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
        this.clock = clock;
    }

    public boolean tryConsume(String key) {
        Instant now = clock.instant();

        WindowState state = attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart.plus(window).isBefore(now)) {
                return new WindowState(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return state.count.get() <= maxAttempts;
    }

    private record WindowState(Instant windowStart, AtomicInteger count) {
    }
}