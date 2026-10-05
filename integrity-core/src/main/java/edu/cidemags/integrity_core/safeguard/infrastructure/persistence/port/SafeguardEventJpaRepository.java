package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardEventEntity;

public interface SafeguardEventJpaRepository extends JpaRepository<SafeguardEventEntity, UUID> {

    boolean existsBySessionIdAndEventId(UUID sessionId, UUID eventId);

    Optional<SafeguardEventEntity> findBySessionIdAndEventId(UUID sessionId, UUID eventId);

    Long countBySessionIdAndEventType(UUID sessionId, SafeguardEventType eventType);

    List<SafeguardEventEntity> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);
}
