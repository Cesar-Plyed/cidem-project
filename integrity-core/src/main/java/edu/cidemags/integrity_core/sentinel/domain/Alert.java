package edu.cidemags.integrity_core.sentinel.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Alerta generada por la politica de escalamiento de Sentinel.
 *
 * <p>{@code alertType} identifica la regla que disparo la alerta (TAB_SWITCH,
 * FOCUS_LOSS, ...) y es la clave usada por la deduplicacion en Redis.
 * {@code examId} permite filtrar las alertas por examen y validar permisos del
 * profesor. Ambos campos son opcionales en el constructor corto para no romper
 * los eventos historicos que no los traian.</p>
 */
public record Alert(
    UUID id,
    ExamSessionId examSessionId,
    StudentId studentId,
    AttemptId attemptId,
    AlertSeverity severity,
    String reason,
    List<UUID> sourceEventIds,
    Instant generatedAt,
    String alertType,
    UUID examId,
    boolean resolved
) {

    public Alert(
        UUID id,
        ExamSessionId examSessionId,
        StudentId studentId,
        AttemptId attemptId,
        AlertSeverity severity,
        String reason,
        List<UUID> sourceEventIds,
        Instant generatedAt
    ) {
        this(id, examSessionId, studentId, attemptId, severity, reason, sourceEventIds, generatedAt,
            "UNKNOWN", null, false);
    }

    public Alert {
        if (id == null) {
            throw new IllegalArgumentException("id es obligatorio");
        }
        if (examSessionId == null) {
            throw new IllegalArgumentException("examSessionId es obligatorio");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId es obligatorio");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity es obligatorio");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason es obligatorio");
        }
        if (sourceEventIds == null || sourceEventIds.isEmpty()) {
            throw new IllegalArgumentException("una alerta debe tener al menos un evento de origen");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt es obligatorio");
        }
        if (attemptId == null) {
            throw new IllegalArgumentException("attemptId es obligatorio");
        }
        if (alertType == null || alertType.isBlank()) {
            throw new IllegalArgumentException("alertType es obligatorio");
        }
    }

    /** Devuelve una copia de la alerta marcada como resuelta. */
    public Alert markResolved() {
        return new Alert(id, examSessionId, studentId, attemptId, severity, reason, sourceEventIds,
            generatedAt, alertType, examId, true);
    }
}
