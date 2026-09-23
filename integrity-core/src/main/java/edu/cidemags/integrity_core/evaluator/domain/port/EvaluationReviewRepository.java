package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationReviewRepository {
    EvaluationReview save(EvaluationReview review);

    Optional<EvaluationReview> findById(UUID id);

    Optional<EvaluationReview> findByAnswerId(UUID answerId);

    List<EvaluationReview> findByAnswerIds(List<UUID> answerIds);

    Optional<EvaluationReview> findByAnswerIdAndReviewerId(UUID answerId, UUID reviewerId);
}
