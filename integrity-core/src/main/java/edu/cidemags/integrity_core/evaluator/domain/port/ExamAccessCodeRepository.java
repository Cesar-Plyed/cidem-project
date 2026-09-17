package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;

import java.util.Optional;
import java.util.UUID;

public interface ExamAccessCodeRepository {
    ExamAccessCode save(ExamAccessCode accessCode);

    Optional<ExamAccessCode> findByExamIdAndCode(UUID examId, String code);

    Optional<ExamAccessCode> findById(UUID id);
}
