package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
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
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        UserRepository users = mock(UserRepository.class);
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams, answers, reviews, users);

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
        when(users.findById(studentId)).thenReturn(Optional.of(new edu.cidemags.integrity_core.auth.domain.model.User(
                studentId, "Juan", "Pérez", "juan@test.com", "hash", null, List.of(), edu.cidemags.integrity_core.auth.domain.model.Role.ALUMNO)));

        var result = useCase.execute(attemptId, studentId, false);

        assertEquals(1, result.score());
        assertEquals(2, result.maxScore());
        org.junit.jupiter.api.Assertions.assertTrue(result.pendingReview());
        assertEquals("juan@test.com", result.studentEmail());
        assertEquals("Juan", result.studentFirstName());
        assertEquals("Pérez", result.studentLastName());
    }

    @Test
    void rejectsResultOfAnotherStudent() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        ExamAttempt attempt = new ExamAttempt(attemptId, examId, studentId, OffsetDateTime.now(), null, true);
        Exam exam = new Exam(examId, "Exam", null, teacherId, OffsetDateTime.now(), List.of(), ExamStatus.PUBLISHED);
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(exams.findById(examId)).thenReturn(Optional.of(exam));
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams,
                mock(ExamAnswerRepository.class), mock(EvaluationReviewRepository.class), mock(UserRepository.class));

        // A random user who is neither student nor teacher tries to access
        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(attemptId, UUID.randomUUID(), false));
    }

    @Test
    void rejectsGlobalResultsForAnotherTeacher() {
        ExamRepository exams = mock(ExamRepository.class);
        UUID ownerId = UUID.randomUUID();
        Exam exam = new Exam(UUID.randomUUID(), "Examen", null, ownerId, OffsetDateTime.now(), List.of(),
                ExamStatus.DRAFT);
        when(exams.findById(exam.getId())).thenReturn(Optional.of(exam));
        GetExamResultUseCase useCase = new GetExamResultUseCase(mock(ExamAttemptRepository.class), exams,
                mock(ExamAnswerRepository.class), mock(EvaluationReviewRepository.class), mock(UserRepository.class));

        assertThrows(AccessDeniedException.class,
                () -> useCase.executeForExam(exam.getId(), UUID.randomUUID(), false));
    }

    @Test
    void calculateAverageAcrossMultipleAttempts() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        ExamAnswerRepository answers = mock(ExamAnswerRepository.class);
        EvaluationReviewRepository reviews = mock(EvaluationReviewRepository.class);
        UserRepository users = mock(UserRepository.class);
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams, answers, reviews, users);

        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID correctOptionId = UUID.randomUUID();
        UUID wrongOptionId = UUID.randomUUID();
        Question choice = new Question(UUID.randomUUID(), "2 + 2", QuestionType.SINGLE_CHOICE,
                List.of(new QuestionOption(correctOptionId, "4", true),
                        new QuestionOption(wrongOptionId, "5", false)),
                null);
        Exam exam = new Exam(examId, "Matematicas", null, teacherId, OffsetDateTime.now(),
                List.of(choice), ExamStatus.PUBLISHED);

        UUID attempt1Id = UUID.randomUUID();
        UUID attempt2Id = UUID.randomUUID();
        UUID student1 = UUID.randomUUID();
        UUID student2 = UUID.randomUUID();
        ExamAttempt attempt1 = new ExamAttempt(attempt1Id, examId, student1, OffsetDateTime.now(),
                OffsetDateTime.now(), true);
        ExamAttempt attempt2 = new ExamAttempt(attempt2Id, examId, student2, OffsetDateTime.now(),
                OffsetDateTime.now(), true);

        when(attempts.findByExamId(examId)).thenReturn(List.of(attempt1, attempt2));
        when(exams.findById(examId)).thenReturn(Optional.of(exam));
        when(answers.findByAttemptId(attempt1Id)).thenReturn(
                List.of(new ExamAnswer(UUID.randomUUID(), attempt1Id, choice.getId(),
                        List.of(correctOptionId), null)));
        when(answers.findByAttemptId(attempt2Id)).thenReturn(
                List.of(new ExamAnswer(UUID.randomUUID(), attempt2Id, choice.getId(),
                        List.of(wrongOptionId), null)));
        when(reviews.findByAnswerId(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        var average = useCase.calculateAverage(examId, teacherId, false);

        assertEquals(2, average.totalAttempts());
        assertEquals(examId, average.examId());
        assertEquals(1, average.maxScore());
        assertEquals(0.5, average.averageScore(), 0.001);
        assertEquals(50.0, average.averagePercentage(), 0.001);
        assertEquals(0, average.pendingReviewCount());
        assertEquals(2, average.gradedAttempts());
    }

    @Test
    void feedbackIsIncludedInQuestionResultForReviewedOpenAnswer() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        ExamAnswerRepository answers = mock(ExamAnswerRepository.class);
        EvaluationReviewRepository reviews = mock(EvaluationReviewRepository.class);
        UserRepository users = mock(UserRepository.class);
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams, answers, reviews, users);

        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID openQuestionId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Question open = new Question(openQuestionId, "Explica", QuestionType.OPEN_ENDED, List.of(), null);
        Exam exam = new Exam(examId, "Test", null, UUID.randomUUID(), OffsetDateTime.now(),
                List.of(open), ExamStatus.PUBLISHED);
        ExamAttempt attempt = new ExamAttempt(attemptId, examId, studentId, OffsetDateTime.now(),
                OffsetDateTime.now(), true);

        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(exams.findById(examId)).thenReturn(Optional.of(exam));
        when(answers.findByAttemptId(attemptId)).thenReturn(
                List.of(new ExamAnswer(answerId, attemptId, openQuestionId, List.of(), "respuesta")));
        when(reviews.findByAnswerId(answerId)).thenReturn(
                Optional.of(new EvaluationReview(UUID.randomUUID(), answerId, UUID.randomUUID(), 9,
                        "Muy bien", OffsetDateTime.now(), OffsetDateTime.now())));

        var result = useCase.execute(attemptId, studentId, false);

        assertEquals(9, result.questions().get(0).score());
        assertEquals("Muy bien", result.questions().get(0).feedback());
        assertFalse(result.pendingReview());
    }

    @Test
    void blocksStudentFromReadingTheAnswerKeyBeforeSubmitting() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        ExamAttempt openAttempt = new ExamAttempt(attemptId, examId, studentId, OffsetDateTime.now(), null, false);
        Exam exam = new Exam(examId, "Exam", null, UUID.randomUUID(), OffsetDateTime.now(), List.of(),
                ExamStatus.PUBLISHED);
        when(attempts.findById(attemptId)).thenReturn(Optional.of(openAttempt));
        when(exams.findById(examId)).thenReturn(Optional.of(exam));
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams,
                mock(ExamAnswerRepository.class), mock(EvaluationReviewRepository.class), mock(UserRepository.class));

        // El resultado incluye isCorrect por opcion y el puntaje en vivo: si un alumno
        // lo leyera antes de entregar, se regalaria la llave de respuestas.
        assertThrows(IllegalStateException.class, () -> useCase.execute(attemptId, studentId, false));
    }

    @Test
    void letsTeacherAndAdminWatchAnOpenAttemptButNotOtherStudents() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        ExamAttempt openAttempt = new ExamAttempt(attemptId, examId, studentId, OffsetDateTime.now(), null, false);
        Exam exam = new Exam(examId, "Exam", null, teacherId, OffsetDateTime.now(), List.of(),
                ExamStatus.PUBLISHED);
        when(attempts.findById(attemptId)).thenReturn(Optional.of(openAttempt));
        when(exams.findById(examId)).thenReturn(Optional.of(exam));
        GetExamResultUseCase useCase = new GetExamResultUseCase(attempts, exams,
                mock(ExamAnswerRepository.class), mock(EvaluationReviewRepository.class), mock(UserRepository.class));

        // Quien escribio el examen (y el administrador) si puede ver el avance en vivo.
        assertEquals(attemptId, useCase.execute(attemptId, teacherId, false).attemptId());
        assertEquals(attemptId, useCase.execute(attemptId, UUID.randomUUID(), true).attemptId());
        // Otro alumno sigue sin poder.
        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(attemptId, UUID.randomUUID(), false));
    }
}
