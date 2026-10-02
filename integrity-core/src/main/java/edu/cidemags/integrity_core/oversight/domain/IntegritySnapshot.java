package edu.cidemags.integrity_core.oversight.domain;

import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

import java.time.Instant;
import java.util.UUID;

public record IntegritySnapshot(
    UUID id,
    ExamSessionId examSessionId,
    StudentId studentId,
    AttemptId attemptId,
    int score,
    RiskLevel riskLevel,
    Instant lastUpdatedAt
) {

    public IntegritySnapshot {
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
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score debe estar entre 0 y 100");
        }
        if (riskLevel == null) {
            throw new IllegalArgumentException("riskLevel es obligatorio");
        }
        if (lastUpdatedAt == null) {
            throw new IllegalArgumentException("lastUpdatedAt es obligatorio");
        }
    }
}