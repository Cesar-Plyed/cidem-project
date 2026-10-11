package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

final class IntegritySnapshotMapper {

    private IntegritySnapshotMapper() {
    }

    static IntegritySnapshotEntity toEntity(IntegritySnapshot snapshot) {
        return new IntegritySnapshotEntity(
            snapshot.id(),
            snapshot.examSessionId().value(),
            snapshot.studentId().value(),
            snapshot.attemptId().value(),
            snapshot.score(),
            snapshot.riskLevel(),
            snapshot.lastUpdatedAt()
        );
    }

    static IntegritySnapshot toDomain(IntegritySnapshotEntity entity) {
        return new IntegritySnapshot(
            entity.getId(),
            new ExamSessionId(entity.getExamSessionId()),
            new StudentId(entity.getStudentId()),
            new AttemptId(entity.getAttemptId()),
            entity.getScore(),
            entity.getRiskLevel(),
            entity.getLastUpdatedAt()
        );
    }
}