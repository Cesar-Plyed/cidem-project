package edu.cidemags.integrity_core.sentinel.domain;

import java.time.Instant;
import java.util.UUID;

public record SuspiciousEvent(
    UUID id,
    ExamSessionId examSessionId,
    StudentId studentId,
    AttemptId attemptId,
    EventType type,
    Instant occurredAt
) {

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