package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface IntegritySnapshotJpaRepository extends JpaRepository<IntegritySnapshotEntity, UUID> {

    Optional<IntegritySnapshotEntity> findByAttemptId(UUID attemptId);
    List<IntegritySnapshotEntity> findByExamSessionId(UUID examSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from IntegritySnapshotEntity s where s.attemptId = :attemptId")
    Optional<IntegritySnapshotEntity> findByAttemptIdForUpdate(@Param("attemptId") UUID attemptId);
}
