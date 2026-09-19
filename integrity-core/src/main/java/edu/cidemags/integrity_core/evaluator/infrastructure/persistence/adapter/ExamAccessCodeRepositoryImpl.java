package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAccessCodeRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAccessCodeEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.ExamAccessCodeJpaRepository;

@Repository
public class ExamAccessCodeRepositoryImpl implements ExamAccessCodeRepository {

    private final ExamAccessCodeJpaRepository repository;

    public ExamAccessCodeRepositoryImpl(ExamAccessCodeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExamAccessCode save(ExamAccessCode accessCode) {
        ExamAccessCodeEntity entity = new ExamAccessCodeEntity(
                accessCode.getId(),
                accessCode.getExamId(),
                accessCode.getCode(),
                accessCode.getExpiresAt(),
                accessCode.isActive());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<ExamAccessCode> findByExamIdAndCode(UUID examId, String code) {
        return repository.findByExamIdAndCode(examId, code).map(this::toDomain);
    }

    @Override
    public Optional<ExamAccessCode> findByCode(String code) {
        return repository.findByCode(code).map(this::toDomain);
    }

    @Override
    public Optional<ExamAccessCode> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    private ExamAccessCode toDomain(ExamAccessCodeEntity entity) {
        return new ExamAccessCode(entity.getId(), entity.getExamId(), entity.getCode(), entity.getExpiresAt(),
                entity.isActive());
    }
}
