package edu.cidemags.integrity_core.sentinel.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento sospechoso detectado durante un intento de examen.
 *
 * <p>Los campos {@code examId}, {@code source} y {@code payload} se agregaron en
 * la V15 para asociar el evento con el examen (filtros de profesor) y conservar
 * el contexto original enviado por el cliente. El constructor de seis
 * argumentos se mantiene por compatibilidad con el codigo existente.</p>
 */
public record SuspiciousEvent(
    UUID id,
    ExamSessionId examSessionId,
    StudentId studentId,
    AttemptId attemptId,
    EventType type,
    Instant occurredAt,
    UUID examId,
    String source,
    String payload
) {

    public SuspiciousEvent(
        UUID id,
        ExamSessionId examSessionId,
        StudentId studentId,
        AttemptId attemptId,
        EventType type,
        Instant occurredAt
    ) {
        this(id, examSessionId, studentId, attemptId, type, occurredAt, null, "SENTINEL", null);
    }

    public SuspiciousEvent {
        if (id == null) {
            throw new IllegalArgumentException("id es obligatorio");
        }
        if (examSessionId == null) {
            throw new IllegalArgumentException("examSessionId es obligatorio");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId es obligatorio");
        }
        if (attemptId == null) {
            throw new IllegalArgumentException("attemptId es obligatorio");
        }
        if (type == null) {
            throw new IllegalArgumentException("type es obligatorio");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt es obligatorio");
        }
    }
}
