package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteExamUseCaseTest {

    @Test
    void deletesPublishedExamWhenItHasNoStudentAttempts() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        DeleteExamUseCase useCase = new DeleteExamUseCase(examRepository, attemptRepository);
        UUID examId = UUID.randomUUID();

        when(examRepository.findById(examId))
                .thenReturn(Optional.of(new Exam(examId, "Examen", null, UUID.randomUUID(),
                        OffsetDateTime.now(), 30, List.of(), ExamStatus.PUBLISHED)));
        when(attemptRepository.existsByExamId(examId)).thenReturn(false);

        useCase.execute(examId);

        verify(examRepository).deleteById(examId);
    }

    @Test
    void preservesExamWhenStudentsHaveAttempts() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        DeleteExamUseCase useCase = new DeleteExamUseCase(examRepository, attemptRepository);
        UUID examId = UUID.randomUUID();

        when(examRepository.findById(examId))
                .thenReturn(Optional.of(new Exam("Examen", null, UUID.randomUUID())));
        when(attemptRepository.existsByExamId(examId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> useCase.execute(examId));

        verify(examRepository, never()).deleteById(examId);
    }
}
