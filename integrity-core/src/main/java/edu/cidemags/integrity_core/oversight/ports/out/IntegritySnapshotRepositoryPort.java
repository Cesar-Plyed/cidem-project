package edu.cidemags.integrity_core.oversight.ports.out;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;

import java.util.List;
import java.util.Optional;

public interface IntegritySnapshotRepositoryPort {

    void save(IntegritySnapshot snapshot);

    Optional<IntegritySnapshot> findByAttemptId(AttemptId attemptId);

    List<IntegritySnapshot> findByExamSessionId(ExamSessionId examSessionId);
}