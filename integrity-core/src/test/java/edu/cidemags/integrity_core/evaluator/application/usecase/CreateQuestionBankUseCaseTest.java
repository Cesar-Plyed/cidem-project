package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionBankCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import edu.cidemags.integrity_core.evaluator.domain.port.QuestionBankRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateQuestionBankUseCaseTest {

    @Test
    void shouldCreateAndPersistQuestionBank() {
        QuestionBankRepository repository = mock(QuestionBankRepository.class);
        CreateQuestionBankUseCase useCase = new CreateQuestionBankUseCase(repository);

        when(repository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionBank bank = useCase.execute(new CreateQuestionBankCommand("Banco 1", "Banco de pruebas"));

        assertNotNull(bank);
        assertEquals("Banco 1", bank.getName());
        assertEquals("Banco de pruebas", bank.getDescription());
        verify(repository).save(any(QuestionBank.class));
    }
}
