package edu.cidemags.integrity_core.events.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento compartido de fin de sesion de examen:
 * se publica al terminar una sesion de examen.
 */
public record ExamSessionFinishedEvent(
        UUID eventId,
        UUID examId,
        UUID attemptId,
        UUID examSessionId,
        UUID studentId,
        Instant occurredAt,
        String source,
        String payload) {
}
