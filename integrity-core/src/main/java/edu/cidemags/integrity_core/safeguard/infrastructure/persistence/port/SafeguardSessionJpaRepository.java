package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;

public interface SafeguardSessionJpaRepository extends JpaRepository<SafeguardSessionEntity, UUID> {

    Optional<SafeguardSessionEntity> findByAttemptId(UUID attemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SafeguardSessionEntity s where s.id = :id")
    Optional<SafeguardSessionEntity> findByIdForUpdate(@Param("id") UUID id);

    List<SafeguardSessionEntity> findByEndedAtIsNullAndLastHeartbeatAtBefore(Instant threshold);
}
