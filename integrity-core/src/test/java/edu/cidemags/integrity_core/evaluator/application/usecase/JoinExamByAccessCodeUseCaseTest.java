package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.JoinExamByAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.DesktopAppRequiredException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamAlreadyAttemptedException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAccessCodeRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamParticipationRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JoinExamByAccessCodeUseCaseTest {

    @Test
    void reusesOpenAttemptForExamRegardlessOfAccessCode() {
        Fixture fixture = new Fixture();
        ExamAttempt openAttempt = new ExamAttempt(fixture.examId, fixture.studentId);
        fixture.givenValidExamAndCode("NEW-CODE");
        when(fixture.attemptRepository.findByStudentId(fixture.studentId)).thenReturn(List.of(openAttempt));

        ExamAttempt result = fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "NEW-CODE", fixture.studentId));

        assertSame(openAttempt, result);
        verify(fixture.participationRepository, never()).reserve(any(), any());
        verify(fixture.attemptRepository, never()).save(any());
    }

    @Test
    void rejectsSecondAttemptWhenAnEarlierAttemptWasSubmitted() {
        Fixture fixture = new Fixture();
        ExamAttempt submitted = new ExamAttempt(UUID.randomUUID(), fixture.examId, fixture.studentId,
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now(), true);
        fixture.givenValidExamAndCode("ANOTHER-CODE");
        when(fixture.attemptRepository.findByStudentId(fixture.studentId)).thenReturn(List.of(submitted));

        assertThrows(ExamAlreadyAttemptedException.class, () -> fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "ANOTHER-CODE", fixture.studentId)));

        verify(fixture.participationRepository, never()).reserve(any(), any());
        verify(fixture.attemptRepository, never()).save(any());
    }

    @Test
    void createsAttemptAfterReservingExamIdAndStudentIdPair() {
        Fixture fixture = new Fixture();
        fixture.givenValidExamAndCode("FIRST-CODE");
        when(fixture.attemptRepository.findByStudentId(fixture.studentId)).thenReturn(List.of());
        when(fixture.participationRepository.reserve(fixture.examId, fixture.studentId)).thenReturn(true);
        when(fixture.attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExamAttempt result = fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "FIRST-CODE", fixture.studentId));

        verify(fixture.participationRepository).reserve(fixture.examId, fixture.studentId);
        verify(fixture.attemptRepository).save(any());
        assertEquals(fixture.examId, result.getExamId());
    }

    @Test
    void returnsAttemptCreatedByConcurrentJoinInsteadOfCreatingAnother() {
        Fixture fixture = new Fixture();
        ExamAttempt concurrentAttempt = new ExamAttempt(fixture.examId, fixture.studentId);
        fixture.givenValidExamAndCode("REGENERATED-CODE");
        when(fixture.attemptRepository.findByStudentId(fixture.studentId))
                .thenReturn(List.of(), List.of(concurrentAttempt));
        when(fixture.participationRepository.reserve(fixture.examId, fixture.studentId)).thenReturn(false);

        ExamAttempt result = fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "REGENERATED-CODE", fixture.studentId));

        assertSame(concurrentAttempt, result);
        verify(fixture.attemptRepository, never()).save(any());
    }

    @Test
    void rejectsBrowserJoinWhenExamRequiresDesktopApp() {
        Fixture fixture = new Fixture();
        fixture.givenValidExamAndCode("BROWSER-CODE", false);

        assertThrows(DesktopAppRequiredException.class, () -> fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "BROWSER-CODE", fixture.studentId), false));

        verify(fixture.participationRepository, never()).reserve(any(), any());
        verify(fixture.attemptRepository, never()).save(any());
    }

    @Test
    void allowsDesktopJoinWhenExamRequiresDesktopApp() {
        Fixture fixture = new Fixture();
        fixture.givenValidExamAndCode("DESKTOP-CODE", false);
        when(fixture.attemptRepository.findByStudentId(fixture.studentId)).thenReturn(List.of());
        when(fixture.participationRepository.reserve(fixture.examId, fixture.studentId)).thenReturn(true);
        when(fixture.attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExamAttempt result = fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "DESKTOP-CODE", fixture.studentId), true);

        assertEquals(fixture.examId, result.getExamId());
    }

    @Test
    void allowsBrowserJoinWhenExamAllowsBrowsers() {
        Fixture fixture = new Fixture();
        fixture.givenValidExamAndCode("OPEN-CODE", true);
        when(fixture.attemptRepository.findByStudentId(fixture.studentId)).thenReturn(List.of());
        when(fixture.participationRepository.reserve(fixture.examId, fixture.studentId)).thenReturn(true);
        when(fixture.attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExamAttempt result = fixture.useCase.execute(
                new JoinExamByAccessCodeCommand(fixture.examId, "OPEN-CODE", fixture.studentId), false);

        assertEquals(fixture.examId, result.getExamId());
    }

    private static final class Fixture {
        private final UUID examId = UUID.randomUUID();
        private final UUID studentId = UUID.randomUUID();
        private final ExamRepository examRepository = mock(ExamRepository.class);
        private final ExamAccessCodeRepository accessCodeRepository = mock(ExamAccessCodeRepository.class);
        private final ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        private final ExamParticipationRepository participationRepository = mock(ExamParticipationRepository.class);
        private final JoinExamByAccessCodeUseCase useCase = new JoinExamByAccessCodeUseCase(
                examRepository, accessCodeRepository, attemptRepository, participationRepository);

        private void givenValidExamAndCode(String code) {
            givenValidExamAndCode(code, true);
        }

        private void givenValidExamAndCode(String code, boolean browserAllowed) {
            Exam exam = new Exam(examId, "Examen", null, UUID.randomUUID(), OffsetDateTime.now(), 30,
                    List.of(), ExamStatus.PUBLISHED, browserAllowed);
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(accessCodeRepository.findByExamIdAndCode(examId, code))
                    .thenReturn(Optional.of(new ExamAccessCode(examId, code, OffsetDateTime.now().plusMinutes(5))));
        }
    }
}
