package edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryRateLimiter {

    /** Llamadas entre barridos de ventanas expiradas. */
    private static final long SWEEP_INTERVAL_CALLS = 512;
    /** Umbral que fuerza un barrido aunque no se alcance el intervalo. */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> attemptsByKey = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong calls = new java.util.concurrent.atomic.AtomicLong();

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
        sweepExpired(now);

        WindowState state = attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart.plus(window).isBefore(now)) {
                return new WindowState(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return state.count.get() <= maxAttempts;
    }

    /**
     * Elimina ventanas expiradas de forma periodica: sin esto, cada IP que
     * nunca vuelve a aparecer deja una entrada permanente en el mapa
     * (crecimiento infinito de memoria bajo spam de IPs ficticias).
     */
    private void sweepExpired(Instant now) {
        if (calls.incrementAndGet() % SWEEP_INTERVAL_CALLS != 0 && attemptsByKey.size() < MAX_TRACKED_KEYS) {
            return;
        }
        attemptsByKey.entrySet().removeIf(entry -> entry.getValue().windowStart().plus(window).isBefore(now));
    }

    private record WindowState(Instant windowStart, AtomicInteger count) {
    }
}