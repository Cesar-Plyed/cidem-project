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
        requireOwnerOrAdmin(exam, requesterId, administrator);

        Map<UUID, Question> openEndedQuestions = exam.getQuestions().stream()
                .filter(q -> q.getType() == QuestionType.OPEN_ENDED)
                .collect(Collectors.toMap(Question::getId, q -> q));
        if (openEndedQuestions.isEmpty()) {
            return List.of();
        }

        List<ExamAttempt> submittedAttempts = examAttemptRepository.findByExamId(examId).stream()
                .filter(ExamAttempt::isSubmitted)
                .toList();
        if (submittedAttempts.isEmpty()) {
            return List.of();
        }

        List<AnswerInContext> contexts = collectOpenEndedAnswers(submittedAttempts, openEndedQuestions);
        if (contexts.isEmpty()) {
            return List.of();
        }

        Map<UUID, EvaluationReview> reviewsByAnswerId = evaluationReviewRepository
                .findByAnswerIds(contexts.stream().map(context -> context.answer().getId()).toList())
                .stream()
                .collect(Collectors.toMap(EvaluationReview::getAnswerId, r -> r));

        return buildResponses(contexts, reviewsByAnswerId, onlyPending);
    }

    private void requireOwnerOrAdmin(Exam exam, UUID requesterId, boolean administrator) {
        boolean isOwner = exam.getCreatedByUserId().equals(requesterId);
        if (!isOwner && !administrator) {
            throw new IllegalStateException("No puedes consultar revisiones de otro profesor");
        }
    }

    /** Una sola lectura de respuestas por intento (antes se recorría dos veces). */
    private List<AnswerInContext> collectOpenEndedAnswers(List<ExamAttempt> submittedAttempts,
            Map<UUID, Question> openEndedQuestions) {
        List<AnswerInContext> contexts = new ArrayList<>();
        for (ExamAttempt attempt : submittedAttempts) {
            for (ExamAnswer answer : examAnswerRepository.findByAttemptId(attempt.getId())) {
                Question question = openEndedQuestions.get(answer.getQuestionId());
                if (question != null) {
                    contexts.add(new AnswerInContext(attempt, answer, question));
                }
            }
        }
        return contexts;
    }

    private List<OpenEndedReviewResponse> buildResponses(List<AnswerInContext> contexts,
            Map<UUID, EvaluationReview> reviewsByAnswerId, boolean onlyPending) {
        List<OpenEndedReviewResponse> results = new ArrayList<>();
        for (AnswerInContext context : contexts) {
            EvaluationReview review = reviewsByAnswerId.get(context.answer().getId());
            if (onlyPending && review != null) {
                continue;
            }
            results.add(toResponse(context.answer(), context.attempt(), context.question(), review));
        }
        return results;
    }

    /** Respuesta de examen abierta con su pregunta y su intento ya resueltos. */
    private record AnswerInContext(ExamAttempt attempt, ExamAnswer answer, Question question) {
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
