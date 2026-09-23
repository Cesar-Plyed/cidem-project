package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewOpenEndedAnswerUseCaseTest {

    @Test
    void shouldPersistManualReviewForOpenEndedAnswer() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ReviewOpenEndedAnswerUseCase useCase = new ReviewOpenEndedAnswerUseCase(
                reviewRepository, answerRepository, attemptRepository, examRepository);

        UUID answerId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        ExamAttempt attempt = new ExamAttempt(attemptId, examId, UUID.randomUUID(),
                OffsetDateTime.now(), null, false);
        Exam exam = new Exam(examId, "Matematicas", null, reviewerId,
                OffsetDateTime.now(), List.of(), ExamStatus.PUBLISHED);
        ExamAnswer answer = new ExamAnswer(answerId, attemptId, UUID.randomUUID(), List.of(), "respuesta");

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(reviewRepository.findByAnswerId(answerId)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(EvaluationReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationReview review = useCase.execute(
                new ReviewOpenEndedAnswerCommand(answerId, reviewerId, 8, "Buena respuesta", true));

        assertNotNull(review);
        assertEquals(answerId, review.getAnswerId());
        assertEquals(8, review.getScore());
        assertEquals("Buena respuesta", review.getFeedback());
        verify(reviewRepository).save(any(EvaluationReview.class));
    }

    @Test
    void shouldUpdateExistingReviewInsteadOfCreatingDuplicate() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ReviewOpenEndedAnswerUseCase useCase = new ReviewOpenEndedAnswerUseCase(
                reviewRepository, answerRepository, attemptRepository, examRepository);

        UUID answerId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ExamAttempt attempt = new ExamAttempt(attemptId, examId, UUID.randomUUID(),
                OffsetDateTime.now(), null, false);
        Exam exam = new Exam(examId, "Historia", null, reviewerId,
                OffsetDateTime.now(), List.of(), ExamStatus.PUBLISHED);
        ExamAnswer answer = new ExamAnswer(answerId, attemptId, UUID.randomUUID(), List.of(), "texto");
        EvaluationReview existing = new EvaluationReview(reviewId, answerId, reviewerId,
                5, "feedback inicial", OffsetDateTime.now(), OffsetDateTime.now());

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(reviewRepository.findByAnswerId(answerId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(EvaluationReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationReview updated = useCase.execute(
                new ReviewOpenEndedAnswerCommand(answerId, reviewerId, 9, "revisado", true));

        assertEquals(reviewId, updated.getId());
        assertEquals(9, updated.getScore());
        assertEquals("revisado", updated.getFeedback());
    }

    @Test
    void shouldRejectReviewByNonOwnerNonAdmin() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ReviewOpenEndedAnswerUseCase useCase = new ReviewOpenEndedAnswerUseCase(
                reviewRepository, answerRepository, attemptRepository, examRepository);

        UUID answerId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID realOwnerId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        ExamAttempt attempt = new ExamAttempt(attemptId, examId, UUID.randomUUID(),
                OffsetDateTime.now(), null, false);
        Exam exam = new Exam(examId, "Matematicas", null, realOwnerId,
                OffsetDateTime.now(), List.of(), ExamStatus.PUBLISHED);
        ExamAnswer answer = new ExamAnswer(answerId, attemptId, UUID.randomUUID(), List.of(), "texto");

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(new ReviewOpenEndedAnswerCommand(answerId, reviewerId, 8,
                        "feedback", false)));
    }

    @Test
    void shouldThrowWhenAnswerNotFound() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ReviewOpenEndedAnswerUseCase useCase = new ReviewOpenEndedAnswerUseCase(
                reviewRepository, answerRepository, attemptRepository, examRepository);

        UUID answerId = UUID.randomUUID();

        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ReviewOpenEndedAnswerCommand(answerId, UUID.randomUUID(),
                        8, "feedback", true)));
    }
}
