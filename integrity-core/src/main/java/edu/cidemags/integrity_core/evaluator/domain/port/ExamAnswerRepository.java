package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAnswerRepository {
    ExamAnswer save(ExamAnswer examAnswer);

    Optional<ExamAnswer> findById(UUID id);

    Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    List<ExamAnswer> findByAttemptId(UUID attemptId);
}
