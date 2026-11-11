package edu.cidemags.integrity_core.events.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento compartido de alerta de Sentinel:
 * se publica al generar una alerta de monitorizacion.
 */
public record SentinelAlertEvent(
        UUID eventId,
        UUID examId,
        UUID attemptId,
        UUID examSessionId,
        UUID studentId,
        Instant occurredAt,
        String source,
        String payload) {
}
