package edu.cidemags.integrity_core.sentinel.application.dto;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista de una alerta lista para la API: incluye el tipo de regla, los nombres
 * de alumno/examen y el estado de resolucion.
 */
public record AlertView(
    UUID id,
    UUID examId,
    UUID attemptId,
    UUID examSessionId,
    UUID studentId,
    String studentName,
    String examName,
    String eventType,
    AlertSeverity severity,
    String reason,
    Instant occurredAt,
    boolean resolved
) {
}
