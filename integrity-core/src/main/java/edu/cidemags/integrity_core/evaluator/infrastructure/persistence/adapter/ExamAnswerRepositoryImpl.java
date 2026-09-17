package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAnswerEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.ExamAnswerJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ExamAnswerRepositoryImpl implements ExamAnswerRepository {

    private final ExamAnswerJpaRepository repository;

    public ExamAnswerRepositoryImpl(ExamAnswerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExamAnswer save(ExamAnswer examAnswer) {
        ExamAnswerEntity entity = new ExamAnswerEntity(
                examAnswer.getId(),
                examAnswer.getAttemptId(),
                examAnswer.getQuestionId(),
                examAnswer.getSelectedOptionIds(),
                examAnswer.getOpenTextAnswer());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<ExamAnswer> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId) {
        return repository.findByAttemptIdAndQuestionId(attemptId, questionId).map(this::toDomain);
    }

    @Override
    public List<ExamAnswer> findByAttemptId(UUID attemptId) {
        return repository.findByAttemptId(attemptId).stream().map(this::toDomain).toList();
    }

    private ExamAnswer toDomain(ExamAnswerEntity entity) {
        return new ExamAnswer(entity.getId(), entity.getAttemptId(), entity.getQuestionId(),
                entity.getSelectedOptionIds(), entity.getOpenTextAnswer());
    }
}
