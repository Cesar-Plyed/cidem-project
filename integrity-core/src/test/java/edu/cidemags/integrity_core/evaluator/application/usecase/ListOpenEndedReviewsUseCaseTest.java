package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.OpenEndedReviewResponse;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListOpenEndedReviewsUseCaseTest {

    @Test
    void shouldListPendingAndReviewedOpenEndedAnswers() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ListOpenEndedReviewsUseCase useCase = new ListOpenEndedReviewsUseCase(
                examRepository, answerRepository, attemptRepository, reviewRepository);

        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID openQuestionId = UUID.randomUUID();
        UUID closedQuestionId = UUID.randomUUID();
        UUID reviewedAnswerId = UUID.randomUUID();
        UUID pendingAnswerId = UUID.randomUUID();

        Question openQ = new Question(openQuestionId, "Explica", QuestionType.OPEN_ENDED, List.of(), null);
        Question closedQ = new Question(closedQuestionId, "2+2", QuestionType.SINGLE_CHOICE,
                List.of(new QuestionOption(UUID.randomUUID(), "4", true)), null);
        Exam exam = new Exam(examId, "Test", "desc", teacherId, OffsetDateTime.now(),
                List.of(openQ), ExamStatus.PUBLISHED);

        ExamAttempt attempt = new ExamAttempt(attemptId, examId, studentId, OffsetDateTime.now(),
                OffsetDateTime.now(), true);

        ExamAnswer reviewedAnswer = new ExamAnswer(reviewedAnswerId, attemptId, openQuestionId,
                List.of(), "respuesta revisada");
        ExamAnswer pendingAnswer = new ExamAnswer(pendingAnswerId, attemptId, openQuestionId,
                List.of(), "respuesta sin revisar");
        ExamAnswer closedAnswer = new ExamAnswer(UUID.randomUUID(), attemptId, closedQuestionId,
                List.of(), null);

        EvaluationReview review = new EvaluationReview(UUID.randomUUID(), reviewedAnswerId, teacherId,
                7, "bien", OffsetDateTime.now(), OffsetDateTime.now());

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByExamId(examId)).thenReturn(List.of(attempt));
        when(answerRepository.findByAttemptId(attemptId))
                .thenReturn(List.of(reviewedAnswer, pendingAnswer, closedAnswer));
        when(reviewRepository.findByAnswerIds(List.of(reviewedAnswerId, pendingAnswerId)))
                .thenReturn(List.of(review));

        List<OpenEndedReviewResponse> result = useCase.execute(examId, teacherId, false, true);

        assertEquals(1, result.size());
        assertEquals(pendingAnswerId, result.get(0).answerId());
        assertFalse(result.get(0).reviewed());

        List<OpenEndedReviewResponse> all = useCase.execute(examId, teacherId, false, false);
        assertEquals(2, all.size());
    }

    @Test
    void shouldRejectNonOwnerNonAdmin() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ListOpenEndedReviewsUseCase useCase = new ListOpenEndedReviewsUseCase(
                examRepository, answerRepository, attemptRepository, reviewRepository);

        UUID ownerId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        Exam exam = new Exam(examId, "Test", "desc", ownerId, OffsetDateTime.now(),
                List.of(), ExamStatus.PUBLISHED);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(examId, otherTeacherId, false, true));
    }

    @Test
    void shouldReturnEmptyWhenNoOpenEndedQuestions() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ListOpenEndedReviewsUseCase useCase = new ListOpenEndedReviewsUseCase(
                examRepository, answerRepository, attemptRepository, reviewRepository);

        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        Question closedQ = new Question(UUID.randomUUID(), "2+2", QuestionType.SINGLE_CHOICE,
                List.of(new QuestionOption(UUID.randomUUID(), "4", true)), null);
        Exam exam = new Exam(examId, "Test", "desc", teacherId, OffsetDateTime.now(),
                List.of(closedQ), ExamStatus.PUBLISHED);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        List<OpenEndedReviewResponse> result = useCase.execute(examId, teacherId, false, true);
        assertEquals(0, result.size());
    }

    @Test
    void shouldAdministerAccessRegardlessOfOwnership() {
        EvaluationReviewRepository reviewRepository = mock(EvaluationReviewRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        ListOpenEndedReviewsUseCase useCase = new ListOpenEndedReviewsUseCase(
                examRepository, answerRepository, attemptRepository, reviewRepository);

        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        Exam exam = new Exam(examId, "Test", "desc", ownerId, OffsetDateTime.now(),
                List.of(), ExamStatus.PUBLISHED);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByExamId(examId)).thenReturn(List.of());

        List<OpenEndedReviewResponse> result = useCase.execute(examId, adminId, true, true);
        assertEquals(0, result.size());
    }
}
