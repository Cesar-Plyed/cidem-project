package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewOpenEndedAnswerUseCase {

    private final EvaluationReviewRepository evaluationReviewRepository;

    public ReviewOpenEndedAnswerUseCase(EvaluationReviewRepository evaluationReviewRepository) {
        this.evaluationReviewRepository = evaluationReviewRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = "exam-attempt-results", allEntries = true)
    public EvaluationReview execute(ReviewOpenEndedAnswerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }

        EvaluationReview review = new EvaluationReview(
                command.answerId(),
                command.reviewerId(),
                command.score(),
                command.feedback());

        return evaluationReviewRepository.save(review);
    }
}
