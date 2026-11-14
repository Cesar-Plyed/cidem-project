package edu.cidemags.integrity_core.sentinel.ports.in;

import edu.cidemags.integrity_core.sentinel.domain.EventType;

import java.time.Instant;
import java.util.UUID;

/**
 * Comando para registrar un evento sospechoso. {@code examId}, {@code source} y
 * {@code payload} son opcionales: el WebSocket no los envia (el examen se
 * resuelve desde {@code AttemptOwnershipPort}) y REST si.
 */
public record ReceiveSuspiciousEventCommand(
    UUID examSessionId,
    UUID studentId,
    UUID attemptId,
    EventType type,
    Instant occurredAt,
    UUID examId,
    String source,
    String payload
) {

    public ReceiveSuspiciousEventCommand(
        UUID examSessionId,
        UUID studentId,
        UUID attemptId,
        EventType type,
        Instant occurredAt
    ) {
        this(examSessionId, studentId, attemptId, type, occurredAt, null, "SENTINEL", null);
    }

    public ReceiveSuspiciousEventCommand(
        UUID examSessionId,
        UUID studentId,
        UUID attemptId,
        EventType type,
        Instant occurredAt,
        UUID examId
    ) {
        this(examSessionId, studentId, attemptId, type, occurredAt, examId, "SENTINEL", null);
    }
}
