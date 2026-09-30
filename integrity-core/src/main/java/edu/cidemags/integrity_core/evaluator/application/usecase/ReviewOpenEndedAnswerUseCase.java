package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewOpenEndedAnswerUseCase {

    private final EvaluationReviewRepository evaluationReviewRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamRepository examRepository;

    public ReviewOpenEndedAnswerUseCase(EvaluationReviewRepository evaluationReviewRepository,
            ExamAnswerRepository examAnswerRepository, ExamAttemptRepository examAttemptRepository,
            ExamRepository examRepository) {
        this.evaluationReviewRepository = evaluationReviewRepository;
        this.examAnswerRepository = examAnswerRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examRepository = examRepository;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "exam-attempt-results", allEntries = true),
        @CacheEvict(cacheNames = "exam-averages", allEntries = true)
    })
    public EvaluationReview execute(ReviewOpenEndedAnswerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }

        var answer = examAnswerRepository.findById(command.answerId())
                .orElseThrow(() -> new IllegalArgumentException("La respuesta no existe"));

        var attempt = examAttemptRepository.findById(answer.getAttemptId())
                .orElseThrow(() -> new ExamNotFoundException(answer.getAttemptId()));

        var exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(attempt.getExamId()));

        boolean isOwner = exam.getCreatedByUserId().equals(command.reviewerId());
        if (!isOwner && !command.administrator()) {
            throw new AccessDeniedException(
                    "Solo el profesor que creo el examen puede revisar respuestas abiertas");
        }

        EvaluationReview review = evaluationReviewRepository.findByAnswerId(command.answerId())
                .map(existing -> existing.withScoreAndFeedback(command.score(), command.feedback(),
                        command.reviewerId()))
                .orElseGet(() -> new EvaluationReview(
                        command.answerId(),
                        command.reviewerId(),
                        command.score(),
                        command.feedback()));

        return evaluationReviewRepository.save(review);
    }
}
