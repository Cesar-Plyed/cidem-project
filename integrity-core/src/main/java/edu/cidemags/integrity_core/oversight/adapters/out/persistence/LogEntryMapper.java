package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

final class LogEntryMapper {

    private LogEntryMapper() {
    }

    static LogEntryEntity toEntity(LogEntry entry) {
        return new LogEntryEntity(
            entry.id(),
            entry.examSessionId().value(),
            entry.studentId().value(),
            entry.attemptId().value(),
            entry.source(),
            entry.severity(),
            entry.description(),
            entry.occurredAt(),
            entry.recordedAt()
        );
    }

    static LogEntry toDomain(LogEntryEntity entity) {
        return new LogEntry(
            entity.getId(),
            new ExamSessionId(entity.getExamSessionId()),
            new StudentId(entity.getStudentId()),
            new AttemptId(entity.getAttemptId()),
            entity.getSource(),
            entity.getSeverity(),
            entity.getDescription(),
            entity.getOccurredAt(),
            entity.getRecordedAt()
        );
    }
}