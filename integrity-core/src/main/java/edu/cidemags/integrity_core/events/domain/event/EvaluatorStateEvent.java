package edu.cidemags.integrity_core.events.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento compartido de cambio de estado del evaluador:
 * se publica al iniciar/terminar una evaluacion.
 */
public record EvaluatorStateEvent(
        UUID eventId,
        UUID examId,
        UUID attemptId,
        UUID examSessionId,
        UUID studentId,
        Instant occurredAt,
        String source,
        String payload) {
}
