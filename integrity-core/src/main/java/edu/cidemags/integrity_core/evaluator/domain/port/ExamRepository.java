package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;

import java.util.Optional;
import java.util.UUID;

public interface ExamRepository {
    Exam save(Exam exam);

    Optional<Exam> findById(UUID id);
}
