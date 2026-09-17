package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ExamAnswerJpaRepository extends JpaRepository<ExamAnswerEntity, UUID> {
    Optional<ExamAnswerEntity> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    List<ExamAnswerEntity> findByAttemptId(UUID attemptId);
}
