package edu.cidemags.integrity_core.safeguard.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SafeguardSessionRepository extends JpaRepository<SafeguardSession, UUID> {

    Optional<SafeguardSession> findByAttemptId(UUID attemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SafeguardSession s where s.id = :id")
    Optional<SafeguardSession> findByIdForUpdate(@Param("id") UUID id);

    List<SafeguardSession> findByEndedAtIsNullAndLastHeartbeatAtBefore(Instant threshold);
}