package edu.cidemags.integrity_core.sentinel.infrastructure.service;

import edu.cidemags.integrity_core.sentinel.domain.model.EscalationRule;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Contadores temporales de eventos en Redis.
 *
 * <p>Cada tipo de evento usa {@code INCR} sobre
 * {@code sentinel:counter:{eventType}}. En el primer incremento se aplica el
 * TTL de la ventana de la regla; a partir de ahi el contador se conserva
 * hasta que Redis lo elimina. {@link #reset(String)} permite reiniciarlo.</p>
 */
@Service
public class RedisCounterService {

    public static final String KEY_PREFIX = "sentinel:counter:";

    private final StringRedisTemplate redisTemplate;

    public RedisCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Incrementa el contador y devuelve el valor actual con su TTL restante. */
    public Counter increment(String eventType, Duration window) {
        String key = key(eventType);
        Long value = redisTemplate.opsForValue().increment(key);

        if (value != null && value == 1L) {
            redisTemplate.expire(key, window);
        }

        return new Counter(value == null ? 0L : value, ttl(eventType));
    }

    /** Valor actual del contador (0 si no existe). */
    public long current(String eventType) {
        String raw = redisTemplate.opsForValue().get(key(eventType));
        return raw == null ? 0L : Long.parseLong(raw);
    }

    /** TTL restante; {@link Duration#ZERO} si la clave no existe o no tiene TTL. */
    public Duration ttl(String eventType) {
        Long seconds = redisTemplate.getExpire(key(eventType), TimeUnit.SECONDS);
        return seconds == null || seconds < 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
    }

    /** Elimina el contador (reset manual). */
    public void reset(String eventType) {
        redisTemplate.delete(key(eventType));
    }

    /**
     * Incrementa el contador de la regla y responde si se alcanzo el umbral.
     */
    public boolean hasReachedThreshold(EscalationRule rule) {
        return increment(rule.eventType(), rule.window()).value() >= rule.threshold();
    }

    /**
     * Evalua la regla: incrementa y devuelve verdadero si se alcanzo el umbral.
     */
    public boolean evaluate(EscalationRule rule) {
        return hasReachedThreshold(rule);
    }

    public static String key(String eventType) {
        return KEY_PREFIX + eventType.toUpperCase();
    }

    /** Foto del contador: valor actual y TTL restante. */
    public record Counter(long value, Duration ttl) {
    }
}
