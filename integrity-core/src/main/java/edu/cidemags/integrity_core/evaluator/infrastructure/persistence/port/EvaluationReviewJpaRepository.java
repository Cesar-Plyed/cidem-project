package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.EvaluationReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvaluationReviewJpaRepository extends JpaRepository<EvaluationReviewEntity, UUID> {
}
