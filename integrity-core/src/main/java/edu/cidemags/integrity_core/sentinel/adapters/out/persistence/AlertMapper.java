package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

import java.time.Instant;

/** Mapper manual entre {@link Alert} y {@link AlertEntity}. */
public final class AlertMapper {

    private static final String UNKNOWN_ALERT_TYPE = "UNKNOWN";

    private AlertMapper() {
    }

    public static AlertEntity toEntity(Alert alert) {
        return new AlertEntity(
            alert.id(),
            alert.id().toString(),
            alert.alertType() == null ? UNKNOWN_ALERT_TYPE : alert.alertType(),
            alert.examId(),
            alert.examSessionId().value(),
            alert.studentId().value(),
            alert.attemptId().value(),
            alert.severity(),
            alert.reason(),
            alert.sourceEventIds(),
            alert.generatedAt(),
            alert.resolved(),
            Instant.now()
        );
    }

    public static Alert toDomain(AlertEntity entity) {
        return new Alert(
            entity.getId(),
            new ExamSessionId(entity.getExamSessionId()),
            new StudentId(entity.getStudentId()),
            new AttemptId(entity.getAttemptId()),
            entity.getSeverity(),
            entity.getReason(),
            entity.getSourceEventIds(),
            entity.getGeneratedAt(),
            entity.getAlertType(),
            entity.getExamId(),
            entity.isResolved()
        );
    }
}
