package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SuspiciousEventJpaRepository extends JpaRepository<SuspiciousEventEntity, UUID> {

    List<SuspiciousEventEntity> findByExamSessionId(UUID examSessionId);

    List<SuspiciousEventEntity> findByAttemptIdOrderByOccurredAtDesc(UUID attemptId, Pageable pageable);
}
