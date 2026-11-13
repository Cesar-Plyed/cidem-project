package edu.cidemags.integrity_core.infrastructure.session;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamSession;
import edu.cidemags.integrity_core.infrastructure.session.entity.RedisSessionEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que mantiene sesiones de examen en Redis con autosave programado.
 * <p>
 * Cada {@link ExamSession} se serializa como JSON en Redis con TTL configurable.
 * El método {@code autosave()} se invoca cada 30 segundos vía {@code @Scheduled}.
 * </p>
 *
 * @see RedisSessionEntity
 * @see RedisSessionConfig
 */
@Service
public class RedisSessionService {

    private final StringRedisTemplate redisTemplate;
    private final Map<UUID, ExamSession> sessionCache = new ConcurrentHashMap<>();

    public RedisSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Guarda o actualiza una sesión en Redis.
     */
    public void saveSession(ExamSession session) {
        String key = "cidem:session:" + session.id();
        String json = serialize(session);
        redisTemplate.opsForValue().set(key, json, session.maxDuration(), TimeUnit.SECONDS);
        sessionCache.put(session.id(), session);
    }

    /**
     * Lee una sesión desde Redis, devolviendo {@code null} si expiró o no existe.
     */
    public ExamSession getSession(UUID examSessionId) {
        String key = "cidem:session:" + examSessionId;
        String json = redisTemplate.opsForValue().get(key);
        return (json != null) ? deserialize(json) : null;
    }

    /**
     * Remueve una sesión (expiración manual o finalización).
     */
    public void removeSession(UUID examSessionId) {
        redisTemplate.delete("cidem:session:" + examSessionId);
        sessionCache.remove(examSessionId);
    }

    /**
     * Lista todas las sesiones activas en Redis.
     */
    public List<ExamSession> listActiveSessions() {
        return sessionCache.values().stream()
                .filter(s -> !s.finished())
                .toList();
    }

    /**
     * Autosave programado: re-escribe todas las sesiones en Redis cada 30 segundos.
     */
    @Scheduled(fixedRate = 30_000)
    public void autosave() {
        sessionCache.forEach((id, session) -> {
            if (!session.finished()) {
                String key = "cidem:session:" + id;
                redisTemplate.opsForValue().set(key, serialize(session), session.maxDuration(), TimeUnit.SECONDS);
            }
        });
    }

    /**
     * Publica un heartbeat de sesión a Redis (TTL corto, ~10 s).
     */
    public void publishHeartbeat(UUID examSessionId) {
        String key = "cidem:heartbeat:" + examSessionId;
        redisTemplate.opsForValue().set(key, Instant.now().toString(), 10, TimeUnit.SECONDS);
    }

    /**
     * Verifica si una sesión está activa (heartbeat recibido en los últimos 15 s).
     */
    public boolean isSessionAlive(UUID examSessionId) {
        String key = "cidem:heartbeat:" + examSessionId;
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0;
    }

    private String serialize(ExamSession session) {
        // JSON via Jackson — usa la configuración global de Spring Boot
        return """
                {"id":"%s","examId":"%s","attemptId":"%s","status":"%s","startedAt":"%s","maxDuration":%d}
                """.formatted(
                session.id(),
                session.examId(),
                session.attemptId(),
                session.status(),
                session.startedAt().toString(),
                session.maxDuration()
        );
    }

    private ExamSession deserialize(String json) {
        // Parseo manual mínimo para esta demo; en producción usar Jackson
        String id = extract(json, "id");
        String examId = extract(json, "examId");
        String attemptId = extract(json, "attemptId");
        String status = extract(json, "status");
        String startedAt = extract(json, "startedAt");
        long maxDuration = Long.parseLong(extract(json, "maxDuration"));
        return ExamSession.from(id, examId, attemptId, status, startedAt, maxDuration);
    }

    private static String extract(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":");
        if (idx < 0) return "";
        int start = json.indexOf('"', idx + key.length() + 2);
        int end = json.indexOf('"', start + 1);
        return json.substring(start + 1, end);
    }
}
