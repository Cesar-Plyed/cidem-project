package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import edu.cidemags.integrity_core.shared.events.ExamCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateExamUseCaseTest {

    @Test
    void shouldCreateAndPersistExam() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CreateExamUseCase useCase = new CreateExamUseCase(examRepository, publisher);
        UUID teacherId = UUID.randomUUID();

        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exam exam = useCase.execute(new CreateExamCommand("Matemáticas", "Examen de prueba", teacherId));

        assertNotNull(exam);
        assertEquals("Matemáticas", exam.getTitle());
        assertEquals(teacherId, exam.getCreatedByUserId());
        assertEquals(ExamStatus.DRAFT, exam.getStatus());
        verify(examRepository).save(any(Exam.class));
    }

    @Test
    void preservesRequestedDuration() {
        ExamRepository examRepository = mock(ExamRepository.class);
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateExamUseCase useCase = new CreateExamUseCase(examRepository, mock(ApplicationEventPublisher.class));

        Exam exam = useCase.execute(new CreateExamCommand("Matemáticas", "Examen de prueba",
                UUID.randomUUID(), List.of(), 47));

        assertEquals(47, exam.getDurationMinutes());
    }

    @Test
    void publishesExamCreatedEventAfterSaving() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CreateExamUseCase useCase = new CreateExamUseCase(examRepository, publisher);
        UUID teacherId = UUID.randomUUID();

        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exam exam = useCase.execute(new CreateExamCommand("Matemáticas", "Examen de prueba", teacherId));

        // SAFEGUARD escucha este evento para siembra su configuración por defecto.
        verify(publisher).publishEvent(any(ExamCreatedEvent.class));
        assertEquals(teacherId, exam.getCreatedByUserId());
    }

    @Test
    void rejectsExamWhoseQuestionPointsExceedTen() {
        CreateExamUseCase useCase = new CreateExamUseCase(mock(ExamRepository.class),
                mock(ApplicationEventPublisher.class));
        UUID teacherId = UUID.randomUUID();

        List<CreateQuestionCommand> heavyQuestions = List.of(
                openQuestion("Pregunta A", 6),
                openQuestion("Pregunta B", 6));

        // La nota máxima del examen es 10: dos preguntas de 6 no caben en la escala.
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new CreateExamCommand("Matemáticas", "Examen de prueba", teacherId, heavyQuestions)));
    }

    private static CreateQuestionCommand openQuestion(String prompt, int points) {
        return new CreateQuestionCommand(prompt, QuestionType.OPEN_ENDED, List.of(), null, points);
    }
}
