package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

final class AlertMapper {

    private AlertMapper() {
    }

    static AlertEntity toEntity(Alert alert) {
        return new AlertEntity(
            alert.id(),
            alert.examSessionId().value(),
            alert.studentId().value(),
            alert.attemptId().value(),
            alert.severity(),
            alert.reason(),
            alert.sourceEventIds(),
            alert.generatedAt()
        );
    }

    static Alert toDomain(AlertEntity entity) {
        return new Alert(
            entity.getId(),
            new ExamSessionId(entity.getExamSessionId()),
            new StudentId(entity.getStudentId()),
            new AttemptId(entity.getAttemptId()),
            entity.getSeverity(),
            entity.getReason(),
            entity.getSourceEventIds(),
            entity.getGeneratedAt()
        );
    }
}