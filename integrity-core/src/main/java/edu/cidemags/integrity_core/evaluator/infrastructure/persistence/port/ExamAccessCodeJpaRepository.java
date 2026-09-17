package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAccessCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamAccessCodeJpaRepository extends JpaRepository<ExamAccessCodeEntity, UUID> {
    Optional<ExamAccessCodeEntity> findByExamIdAndCode(UUID examId, String code);
}
