package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAttemptEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.ExamAttemptJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ExamAttemptRepositoryImpl implements ExamAttemptRepository {

    private final ExamAttemptJpaRepository repository;

    public ExamAttemptRepositoryImpl(ExamAttemptJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExamAttempt save(ExamAttempt examAttempt) {
        ExamAttemptEntity entity = new ExamAttemptEntity(
                examAttempt.getId(),
                examAttempt.getExamId(),
                examAttempt.getStudentId(),
                examAttempt.getStartedAt(),
                examAttempt.getSubmittedAt(),
                examAttempt.isSubmitted(),
                examAttempt.isIntegrityFailed());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<ExamAttempt> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ExamAttempt> findByStudentId(UUID studentId) {
        return repository.findByStudentId(studentId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ExamAttempt> findByExamId(UUID examId) {
        return repository.findByExamId(examId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByExamId(UUID examId) {
        return repository.existsByExamId(examId);
    }

    private ExamAttempt toDomain(ExamAttemptEntity entity) {
        return new ExamAttempt(
                entity.getId(),
                entity.getExamId(),
                entity.getStudentId(),
                entity.getStartedAt(),
                entity.getSubmittedAt(),
                entity.isSubmitted(),
                entity.isIntegrityFailed());
    }
}
