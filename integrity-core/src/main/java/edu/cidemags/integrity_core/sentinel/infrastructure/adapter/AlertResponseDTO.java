package edu.cidemags.integrity_core.sentinel.infrastructure.adapter;

import edu.cidemags.integrity_core.sentinel.application.dto.AlertView;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta REST de una alerta de Sentinel. Incluye nombres resueltos para que
 * la pantalla de profesores no tenga que hacer consultas adicionales.
 */
public record AlertResponseDTO(
    UUID id,
    UUID examId,
    UUID attemptId,
    UUID examSessionId,
    UUID studentId,
    String studentName,
    String examName,
    String eventType,
    String severity,
    String reason,
    Instant occurredAt,
    boolean resolved
) {

    public static AlertResponseDTO from(AlertView view) {
        return new AlertResponseDTO(
            view.id(),
            view.examId(),
            view.attemptId(),
            view.examSessionId(),
            view.studentId(),
            view.studentName(),
            view.examName(),
            view.eventType(),
            view.severity() == null ? null : view.severity().name(),
            view.reason(),
            view.occurredAt(),
            view.resolved()
        );
    }
}
