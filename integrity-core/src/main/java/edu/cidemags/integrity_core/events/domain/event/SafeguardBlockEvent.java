package edu.cidemags.integrity_core.events.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento compartido de bloque de SafeGuard:
 * se publica cuando un examen se bloquea por politica de integridad.
 */
public record SafeguardBlockEvent(
        UUID eventId,
        UUID examId,
        UUID attemptId,
        UUID examSessionId,
        UUID studentId,
        Instant occurredAt,
        String source,
        String payload) {
}
