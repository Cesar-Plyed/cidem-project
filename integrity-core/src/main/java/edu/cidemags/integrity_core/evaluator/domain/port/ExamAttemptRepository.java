package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;

import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository {
    ExamAttempt save(ExamAttempt examAttempt);

    Optional<ExamAttempt> findById(UUID id);
}
