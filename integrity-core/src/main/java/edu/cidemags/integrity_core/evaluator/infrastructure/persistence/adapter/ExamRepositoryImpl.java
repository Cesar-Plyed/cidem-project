package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.mapper.ExamMapper;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port.ExamJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ExamRepositoryImpl implements ExamRepository {

    private final ExamJpaRepository examJpaRepository;
    private final ExamMapper examMapper;

    public ExamRepositoryImpl(ExamJpaRepository examJpaRepository, ExamMapper examMapper) {
        this.examJpaRepository = examJpaRepository;
        this.examMapper = examMapper;
    }

    @Override
    public Exam save(Exam exam) {
        ExamEntity entity = examMapper.toEntity(exam);
        ExamEntity saved = examJpaRepository.save(entity);
        return examMapper.toDomain(saved);
    }

    @Override
    public Optional<Exam> findById(UUID id) {
        return examJpaRepository.findById(id).map(examMapper::toDomain);
    }
}
