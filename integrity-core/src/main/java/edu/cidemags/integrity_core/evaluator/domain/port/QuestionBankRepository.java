package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;

import java.util.Optional;
import java.util.UUID;

public interface QuestionBankRepository {
    QuestionBank save(QuestionBank questionBank);

    Optional<QuestionBank> findById(UUID id);
}
