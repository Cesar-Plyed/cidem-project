package edu.cidemags.integrity_core.events.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento compartido de inicio de sesion de examen:
 * se publica al iniciar una sesion de examen.
 */
public record ExamSessionStartedEvent(
        UUID eventId,
        UUID examId,
        UUID attemptId,
        UUID examSessionId,
        UUID studentId,
        Instant occurredAt,
        String source,
        String payload) {
}
