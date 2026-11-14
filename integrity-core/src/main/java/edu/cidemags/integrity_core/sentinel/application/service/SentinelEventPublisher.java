package edu.cidemags.integrity_core.sentinel.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cidemags.integrity_core.events.domain.event.SentinelAlertEvent;
import edu.cidemags.integrity_core.sentinel.application.event.AlertGeneratedEvent;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publica las alertas de Sentinel como eventos de Spring y deduplica las
 * repetidas.
 *
 * <p>La deduplicacion vive aqui (no en los listeners): una misma alerta
 * ({@code alert_type} + {@code examId} + {@code attemptId}) no se vuelve a
 * publicar hasta que expire la ventana de 5 minutos en Redis. La clave es
 * {@code dedup:{alert_type}:{examId}:{attemptId}}.</p>
 */
@Service
public class SentinelEventPublisher {

    static final String DEDUP_KEY_PREFIX = "dedup:";
    static final Duration DEDUP_WINDOW = Duration.ofMinutes(5);

    private static final Logger log = LoggerFactory.getLogger(SentinelEventPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public SentinelEventPublisher(
        StringRedisTemplate redisTemplate,
        ApplicationEventPublisher eventPublisher,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica la alerta si no se publico una equivalente en la ventana de
     * deduplicacion.
     *
     * @return {@code true} si se publico; {@code false} si era duplicada.
     */
    public boolean publish(Alert alert) {
        String alertType = alert.alertType() == null || alert.alertType().isBlank()
            ? "UNKNOWN"
            : alert.alertType();
        String examId = alert.examId() == null ? "unknown" : alert.examId().toString();
        String dedupKey = "%s%s:%s:%s".formatted(
            DEDUP_KEY_PREFIX, alertType, examId, alert.attemptId().value());

        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(dedupKey, "1", DEDUP_WINDOW);

        if (!Boolean.TRUE.equals(isNew)) {
            log.info("Alerta duplicada ({}), no se publica de nuevo", dedupKey);
            return false;
        }

        eventPublisher.publishEvent(new AlertGeneratedEvent(alert));
        eventPublisher.publishEvent(toSharedEvent(alert));

        log.debug("Alerta publicada con clave de deduplicacion {}", dedupKey);
        return true;
    }

    private SentinelAlertEvent toSharedEvent(Alert alert) {
        return new SentinelAlertEvent(
            alert.id(),
            alert.examId(),
            alert.attemptId().value(),
            alert.examSessionId().value(),
            alert.studentId().value(),
            alert.generatedAt(),
            "SENTINEL",
            serializePayload(alert)
        );
    }

    private String serializePayload(Alert alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertType", alert.alertType());
        payload.put("severity", alert.severity().name());
        payload.put("reason", alert.reason());
        payload.put("sourceEventIds", alert.sourceEventIds());
        payload.put("resolved", alert.resolved());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el payload de la alerta", ex);
        }
    }
}
