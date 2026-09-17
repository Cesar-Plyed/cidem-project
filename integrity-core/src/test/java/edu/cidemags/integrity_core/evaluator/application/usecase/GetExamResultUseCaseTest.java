package edu.cidemags.integrity_core.evaluator.application.usecase;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetExamResultUseCaseTest {
    @Test
    void calculatesClosedScoreAndMarksOpenAnswerAsPending() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        ExamAnswerRepository answers = mock(ExamAnswerRepository.class);
        EvaluationReviewRepository reviews = mock(EvaluationReviewRepository.class);
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams, answers, reviews);

        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID correctOptionId = UUID.randomUUID();
        UUID choiceId = UUID.randomUUID();
        UUID openId = UUID.randomUUID();
        Question choice = new Question(UUID.randomUUID(), "2 + 2", QuestionType.SINGLE_CHOICE,
                List.of(new QuestionOption(correctOptionId, "4", true), new QuestionOption(choiceId, "5", false)),
                null);
        Question open = new Question(openId, "Explica", QuestionType.OPEN_ENDED, List.of(), null);
        Exam exam = new Exam(examId, "Matematicas", null, UUID.randomUUID(), OffsetDateTime.now(),
                List.of(choice, open), ExamStatus.PUBLISHED);
        ExamAttempt attempt = new ExamAttempt(attemptId, examId, studentId, OffsetDateTime.now(), null, true);

        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(exams.findById(examId)).thenReturn(Optional.of(exam));
        when(answers.findByAttemptId(attemptId)).thenReturn(List.of(
                new ExamAnswer(UUID.randomUUID(), attemptId, choice.getId(), List.of(correctOptionId), null),
                new ExamAnswer(UUID.randomUUID(), attemptId, openId, List.of(), "texto")));
        when(reviews.findByAnswerId(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        var result = useCase.execute(attemptId, studentId);

        assertEquals(1, result.score());
        assertEquals(2, result.maxScore());
        org.junit.jupiter.api.Assertions.assertTrue(result.pendingReview());
    }

    @Test
    void rejectsResultOfAnotherStudent() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamAttempt attempt = new ExamAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now(), null, true);
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, mock(ExamRepository.class),
                mock(ExamAnswerRepository.class), mock(EvaluationReviewRepository.class));

        assertThrows(IllegalStateException.class, () -> useCase.execute(attempt.getId(), UUID.randomUUID()));
    }

    @Test
    void rejectsGlobalResultsForAnotherTeacher() {
        ExamRepository exams = mock(ExamRepository.class);
        UUID ownerId = UUID.randomUUID();
        Exam exam = new Exam(UUID.randomUUID(), "Examen", null, ownerId, OffsetDateTime.now(), List.of(),
                ExamStatus.DRAFT);
        when(exams.findById(exam.getId())).thenReturn(Optional.of(exam));
        GetExamResultUseCase useCase = new GetExamResultUseCase(mock(ExamAttemptRepository.class), exams,
                mock(ExamAnswerRepository.class), mock(EvaluationReviewRepository.class));

        assertThrows(IllegalStateException.class,
                () -> useCase.executeForExam(exam.getId(), UUID.randomUUID(), false));
    }
}