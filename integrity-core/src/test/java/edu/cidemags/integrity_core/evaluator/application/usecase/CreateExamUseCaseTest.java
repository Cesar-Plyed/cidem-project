package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateExamUseCaseTest {

    @Test
    void shouldCreateAndPersistExam() {
        ExamRepository examRepository = mock(ExamRepository.class);
        CreateExamUseCase useCase = new CreateExamUseCase(examRepository);
        UUID teacherId = UUID.randomUUID();

        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exam exam = useCase.execute(new CreateExamCommand("Matemáticas", "Examen de prueba", teacherId));

        assertNotNull(exam);
        assertEquals("Matemáticas", exam.getTitle());
        assertEquals(teacherId, exam.getCreatedByUserId());
        assertEquals(ExamStatus.DRAFT, exam.getStatus());
        verify(examRepository).save(any(Exam.class));
    }
}
