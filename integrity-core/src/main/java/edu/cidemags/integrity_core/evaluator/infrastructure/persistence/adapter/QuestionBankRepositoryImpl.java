package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import edu.cidemags.integrity_core.evaluator.domain.port.QuestionBankRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.QuestionBankEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.mapper.QuestionMapper;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.QuestionBankJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public class QuestionBankRepositoryImpl implements QuestionBankRepository {

    private final QuestionBankJpaRepository repository;
    private final QuestionMapper questionMapper;

    public QuestionBankRepositoryImpl(QuestionBankJpaRepository repository, QuestionMapper questionMapper) {
        this.repository = repository;
        this.questionMapper = questionMapper;
    }

    @Override
    public QuestionBank save(QuestionBank questionBank) {
        QuestionBankEntity entity = new QuestionBankEntity(questionBank.getId(), questionBank.getName(),
                questionBank.getDescription(), questionBank.getCreatedByUserId());
        entity.setQuestions(questionBank.getQuestions().stream().map(questionMapper::toEntity).toList());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<QuestionBank> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<QuestionBank> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private QuestionBank toDomain(QuestionBankEntity entity) {
        return new QuestionBank(entity.getId(), entity.getName(), entity.getDescription(), entity.getCreatedByUserId(),
                entity.getQuestions().stream().map(questionMapper::toDomain).toList());
    }
}
