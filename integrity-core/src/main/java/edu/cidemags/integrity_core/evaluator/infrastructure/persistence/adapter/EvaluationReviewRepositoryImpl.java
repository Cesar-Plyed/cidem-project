package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.EvaluationReviewEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.EvaluationReviewJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EvaluationReviewRepositoryImpl implements EvaluationReviewRepository {

    private final EvaluationReviewJpaRepository repository;

    public EvaluationReviewRepositoryImpl(EvaluationReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public EvaluationReview save(EvaluationReview review) {
        EvaluationReviewEntity entity = new EvaluationReviewEntity(
                review.getId(),
                review.getAnswerId(),
                review.getReviewerId(),
                review.getScore(),
                review.getFeedback(),
                review.getCreatedAt(),
                review.getUpdatedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<EvaluationReview> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<EvaluationReview> findByAnswerId(UUID answerId) {
        return repository.findByAnswerId(answerId).map(this::toDomain);
    }

    @Override
    public List<EvaluationReview> findByAnswerIds(List<UUID> answerIds) {
        return repository.findByAnswerIdIn(answerIds).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<EvaluationReview> findByAnswerIdAndReviewerId(UUID answerId, UUID reviewerId) {
        return repository.findByAnswerIdAndReviewerId(answerId, reviewerId).map(this::toDomain);
    }

    private EvaluationReview toDomain(EvaluationReviewEntity entity) {
        return new EvaluationReview(entity.getId(), entity.getAnswerId(), entity.getReviewerId(),
                entity.getScore(), entity.getFeedback(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
