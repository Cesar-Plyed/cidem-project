package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.GenerateExamAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAccessCodeRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateExamAccessCodeUseCaseTest {

    @Test
    void shouldGenerateExamAccessCode() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ExamAccessCodeRepository codeRepository = mock(ExamAccessCodeRepository.class);
        GenerateExamAccessCodeUseCase useCase = new GenerateExamAccessCodeUseCase(examRepository, codeRepository);

        UUID examId = UUID.randomUUID();
        Exam exam = new Exam(examId, "Historia", "Examen", UUID.randomUUID(), java.time.OffsetDateTime.now(),
                java.util.Collections.emptyList(), edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(codeRepository.save(any(ExamAccessCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamAccessCode code = useCase.execute(new GenerateExamAccessCodeCommand(examId, "ABCD1234", 30));

        assertNotNull(code);
        assertEquals(examId, code.getExamId());
        assertEquals("ABCD1234", code.getCode());
        verify(codeRepository).save(any(ExamAccessCode.class));
    }
}
