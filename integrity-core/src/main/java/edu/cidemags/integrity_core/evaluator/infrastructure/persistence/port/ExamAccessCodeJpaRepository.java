package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAccessCodeEntity;

public interface ExamAccessCodeJpaRepository extends JpaRepository<ExamAccessCodeEntity, UUID> {
    Optional<ExamAccessCodeEntity> findByExamIdAndCode(UUID examId, String code);

    Optional<ExamAccessCodeEntity> findByCode(String code);
}
