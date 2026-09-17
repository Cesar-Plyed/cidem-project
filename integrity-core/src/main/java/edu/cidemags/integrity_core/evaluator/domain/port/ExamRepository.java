package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository {
    Exam save(Exam exam);

    Optional<Exam> findById(UUID id);

    List<Exam> findAll();

    void deleteById(UUID id);
}
