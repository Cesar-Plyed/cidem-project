package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.UpdateQuestionBankCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.QuestionBankNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption;
import edu.cidemags.integrity_core.evaluator.domain.port.QuestionBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
public class QuestionBankManagementUseCase {
    private final QuestionBankRepository repository;

    public QuestionBankManagementUseCase(QuestionBankRepository repository) {
        this.repository = repository;
    }

    public List<QuestionBank> list() {
        return repository.findAll();
    }

    public List<QuestionBank> listFor(UUID requesterId, boolean administrator) {
        if (administrator) {
            return list();
        }
        return repository.findAll().stream()
                .filter(bank -> requesterId.equals(bank.getCreatedByUserId()))
                .toList();
    }

    public QuestionBank get(UUID bankId) {
        return repository.findById(bankId).orElseThrow(() -> new QuestionBankNotFoundException(bankId));
    }

    public QuestionBank getFor(UUID bankId, UUID requesterId, boolean administrator) {
        QuestionBank bank = get(bankId);
        assertCanManage(bank, requesterId, administrator);
        return bank;
    }

    public void assertCanManage(UUID bankId, UUID requesterId, boolean administrator) {
        assertCanManage(get(bankId), requesterId, administrator);
    }

    private void assertCanManage(QuestionBank bank, UUID requesterId, boolean administrator) {
        if (!administrator && !requesterId.equals(bank.getCreatedByUserId())) {
            throw new AccessDeniedException("No tienes permisos sobre este banco");
        }
    }

    @Transactional
    public QuestionBank update(UpdateQuestionBankCommand command) {
        if (command == null || command.bankId() == null) {
            throw new IllegalArgumentException("El identificador del banco es obligatorio");
        }
        QuestionBank bank = get(command.bankId());
        bank.updateDetails(command.name(), command.description());
        return repository.save(bank);
    }

    @Transactional
    public QuestionBank addQuestion(UUID bankId, CreateQuestionCommand command) {
        if (command == null || command.type() == null) {
            throw new IllegalArgumentException("La pregunta y su tipo son obligatorios");
        }
        QuestionBank bank = get(bankId);
        List<QuestionOption> options = command.options() == null ? List.of()
                : command.options().stream().map(option -> new QuestionOption(option.text(), option.correct()))
                        .toList();
        bank.addQuestion(new Question(UUID.randomUUID(), command.prompt(), command.type(), options,
                command.correctAnswerText()));
        return repository.save(bank);
    }

    @Transactional
    public void delete(UUID bankId) {
        get(bankId);
        repository.deleteById(bankId);
    }
}