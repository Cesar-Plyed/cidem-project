package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamAttemptJpaRepository extends JpaRepository<ExamAttemptEntity, UUID> {
    List<ExamAttemptEntity> findByStudentId(UUID studentId);

    List<ExamAttemptEntity> findByExamId(UUID examId);

    boolean existsByExamId(UUID examId);
}
