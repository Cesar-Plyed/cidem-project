package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.SubmitExamAttemptCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitExamAttemptUseCaseTest {

    @Test
    void repeatedSubmitReturnsExistingAttemptWithoutSavingAgain() {
        ExamAttemptRepository repository = mock(ExamAttemptRepository.class);
        SubmitExamAttemptUseCase useCase = new SubmitExamAttemptUseCase(repository);
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        ExamAttempt attempt = new ExamAttempt(attemptId, UUID.randomUUID(), studentId,
                OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now(), true);
        when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));

        ExamAttempt result = useCase.execute(new SubmitExamAttemptCommand(attemptId), studentId);

        assertSame(attempt, result);
        verify(repository, never()).save(attempt);
    }
}
