package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.OpenEndedReviewResponse;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListOpenEndedReviewsUseCase {

    private final ExamRepository examRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final EvaluationReviewRepository evaluationReviewRepository;

    public ListOpenEndedReviewsUseCase(ExamRepository examRepository,
            ExamAnswerRepository examAnswerRepository,
            ExamAttemptRepository examAttemptRepository,
            EvaluationReviewRepository evaluationReviewRepository) {
        this.examRepository = examRepository;
        this.examAnswerRepository = examAnswerRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.evaluationReviewRepository = evaluationReviewRepository;
    }

    @Transactional(readOnly = true)
    public List<OpenEndedReviewResponse> execute(UUID examId, UUID requesterId, boolean administrator,
            boolean onlyPending) {
        if (examId == null || requesterId == null) {
            throw new IllegalArgumentException("El examen y el usuario son obligatorios");
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));

        boolean isOwner = exam.getCreatedByUserId().equals(requesterId);
        if (!isOwner && !administrator) {
            throw new IllegalStateException("No puedes consultar revisiones de otro profesor");
        }

        Map<UUID, Question> openEndedQuestions = exam.getQuestions().stream()
                .filter(q -> q.getType() == QuestionType.OPEN_ENDED)
                .collect(Collectors.toMap(Question::getId, q -> q));

        if (openEndedQuestions.isEmpty()) {
            return List.of();
        }

        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        if (attempts.isEmpty()) {
            return List.of();
        }

        List<UUID> allAnswerIds = new ArrayList<>();
        List<ExamAttempt> nonSubmittedAttempts = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            if (!attempt.isSubmitted()) {
                nonSubmittedAttempts.add(attempt);
                continue;
            }
            List<ExamAnswer> answers = examAnswerRepository.findByAttemptId(attempt.getId());
            for (ExamAnswer answer : answers) {
                if (openEndedQuestions.containsKey(answer.getQuestionId())) {
                    allAnswerIds.add(answer.getId());
                }
            }
        }

        if (allAnswerIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, EvaluationReview> reviewsByAnswerId = evaluationReviewRepository.findByAnswerIds(allAnswerIds)
                .stream()
                .collect(Collectors.toMap(EvaluationReview::getAnswerId, r -> r));

        List<OpenEndedReviewResponse> results = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            if (!attempt.isSubmitted()) {
                continue;
            }
            List<ExamAnswer> answers = examAnswerRepository.findByAttemptId(attempt.getId());
            for (ExamAnswer answer : answers) {
                Question question = openEndedQuestions.get(answer.getQuestionId());
                if (question == null) {
                    continue;
                }
                EvaluationReview review = reviewsByAnswerId.get(answer.getId());
                boolean reviewed = review != null;
                if (onlyPending && reviewed) {
                    continue;
                }
                results.add(toResponse(answer, attempt, question, review));
            }
        }

        return results;
    }

    private OpenEndedReviewResponse toResponse(ExamAnswer answer, ExamAttempt attempt, Question question,
            EvaluationReview review) {
        return new OpenEndedReviewResponse(
                answer.getId(),
                attempt.getId(),
                attempt.getStudentId(),
                question.getId(),
                question.getPrompt(),
                answer.getOpenTextAnswer(),
                review != null ? review.getScore() : null,
                review != null ? review.getFeedback() : null,
                review != null ? review.getReviewerId() : null,
                review != null,
                review != null ? review.getCreatedAt() : null,
                review != null ? review.getUpdatedAt() : null);
    }
}
