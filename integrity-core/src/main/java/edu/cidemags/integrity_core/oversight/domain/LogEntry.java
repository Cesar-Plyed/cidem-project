package edu.cidemags.integrity_core.oversight.domain;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

import java.time.Instant;
import java.util.UUID;

public record LogEntry(
    UUID id,
    ExamSessionId examSessionId,
    StudentId studentId,
    AttemptId attemptId,
    LogSource source,
    AlertSeverity severity,
    String description,
    Instant occurredAt,
    Instant recordedAt
) {

    public LogEntry {
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
        if (source == null) {
            throw new IllegalArgumentException("source es obligatorio");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity es obligatorio");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description es obligatorio");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt es obligatorio");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt es obligatorio");
        }
    }
}