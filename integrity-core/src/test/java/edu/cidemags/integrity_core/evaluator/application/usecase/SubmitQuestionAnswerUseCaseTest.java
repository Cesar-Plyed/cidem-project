package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.SubmitQuestionAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitQuestionAnswerUseCaseTest {

    @Test
    void shouldSaveStudentAnswer() {
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        ExamAnswerRepository answerRepository = mock(ExamAnswerRepository.class);
        SubmitQuestionAnswerUseCase useCase = new SubmitQuestionAnswerUseCase(attemptRepository, answerRepository);

        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        ExamAttempt attempt = new ExamAttempt(attemptId, UUID.randomUUID(), UUID.randomUUID(),
                java.time.OffsetDateTime.now(), null, false);

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamAnswer answer = useCase.execute(
                new SubmitQuestionAnswerCommand(attemptId, questionId, List.of(UUID.randomUUID()), "respuesta libre"));

        assertNotNull(answer);
        assertEquals(attemptId, answer.getAttemptId());
        assertEquals(questionId, answer.getQuestionId());
        assertEquals("respuesta libre", answer.getOpenTextAnswer());
        verify(answerRepository).save(any(ExamAnswer.class));
    }
}
