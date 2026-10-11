package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LogEntryJpaRepository extends JpaRepository<LogEntryEntity, UUID> {

    List<LogEntryEntity> findByExamSessionId(UUID examSessionId);

    List<LogEntryEntity> findByAttemptId(UUID attemptId);

    boolean existsByDedupKey(String dedupKey);
}