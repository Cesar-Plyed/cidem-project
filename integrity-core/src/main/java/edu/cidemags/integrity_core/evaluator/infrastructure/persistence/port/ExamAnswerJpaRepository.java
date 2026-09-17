package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamAnswerJpaRepository extends JpaRepository<ExamAnswerEntity, UUID> {
}
