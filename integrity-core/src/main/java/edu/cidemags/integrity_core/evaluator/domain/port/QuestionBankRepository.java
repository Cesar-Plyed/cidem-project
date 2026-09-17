package edu.cidemags.integrity_core.evaluator.domain.port;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface QuestionBankRepository {
    QuestionBank save(QuestionBank questionBank);

    Optional<QuestionBank> findById(UUID id);

    List<QuestionBank> findAll();

    void deleteById(UUID id);
}
