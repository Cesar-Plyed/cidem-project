package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import edu.cidemags.integrity_core.evaluator.domain.port.QuestionBankRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.QuestionBankEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.QuestionBankJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class QuestionBankRepositoryImpl implements QuestionBankRepository {

    private final QuestionBankJpaRepository repository;

    public QuestionBankRepositoryImpl(QuestionBankJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public QuestionBank save(QuestionBank questionBank) {
        QuestionBankEntity entity = new QuestionBankEntity(questionBank.getId(), questionBank.getName(),
                questionBank.getDescription());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<QuestionBank> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    private QuestionBank toDomain(QuestionBankEntity entity) {
        return new QuestionBank(entity.getId(), entity.getName(), entity.getDescription(),
                java.util.Collections.emptyList());
    }
}
