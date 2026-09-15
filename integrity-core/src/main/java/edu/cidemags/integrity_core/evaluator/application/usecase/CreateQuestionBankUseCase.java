package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionBankCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import edu.cidemags.integrity_core.evaluator.domain.port.QuestionBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateQuestionBankUseCase {

    private final QuestionBankRepository questionBankRepository;

    public CreateQuestionBankUseCase(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Transactional
    public QuestionBank execute(CreateQuestionBankCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        QuestionBank questionBank = new QuestionBank(command.name(), command.description());
        return questionBankRepository.save(questionBank);
    }
}
