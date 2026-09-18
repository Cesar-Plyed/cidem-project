package edu.cidemags.integrity_core.sentinel.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Alert(
    UUID id,
    ExamSessionId examSessionId,
    StudentId studentId,
    AlertSeverity severity,
    String reason,
    List<UUID> sourceEventIds,
    Instant generatedAt
) {

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
    }
}