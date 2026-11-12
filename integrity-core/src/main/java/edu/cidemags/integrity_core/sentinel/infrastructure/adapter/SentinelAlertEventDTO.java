package edu.cidemags.integrity_core.sentinel.infrastructure.adapter;

import edu.cidemags.integrity_core.sentinel.domain.EventType;

import java.time.Instant;
import java.util.UUID;

/**
 * Body de POST /sentinel/events. Mantiene el nombre del contrato compartido
 * {@code SentinelAlertEvent}, pero valida solo lo imprescindible: el evento
 * lo origina el cliente (SafeGuard/Evaluator/Sentinel).
 */
public record SentinelAlertEventDTO(
    UUID eventId,
    UUID examId,
    UUID examSessionId,
    UUID attemptId,
    UUID studentId,
    EventType eventType,
    String source,
    String payload,
    String severity,
    String reason,
    Instant occurredAt
) {

    public SentinelAlertEventDTO {
        if (examId == null) {
            throw new IllegalArgumentException("examId es obligatorio");
        }
        if (attemptId == null) {
            throw new IllegalArgumentException("attemptId es obligatorio");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType es obligatorio");
        }
    }
}
