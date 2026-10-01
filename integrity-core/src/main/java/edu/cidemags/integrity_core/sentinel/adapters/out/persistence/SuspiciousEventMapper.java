package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;

final class SuspiciousEventMapper {

    private SuspiciousEventMapper() {
    }

    static SuspiciousEventEntity toEntity(SuspiciousEvent event) {
        return new SuspiciousEventEntity(
            event.id(),
            event.examSessionId().value(),
            event.studentId().value(),
            event.attemptId().value(),
            event.type(),
            event.occurredAt()
        );
    }

    static SuspiciousEvent toDomain(SuspiciousEventEntity entity) {
        return new SuspiciousEvent(
            entity.getId(),
            new ExamSessionId(entity.getExamSessionId()),
            new StudentId(entity.getStudentId()),
            new AttemptId(entity.getAttemptId()),
            entity.getType(),
            entity.getOccurredAt()
        );
    }
}