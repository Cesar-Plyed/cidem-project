package edu.cidemags.integrity_core.safeguard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardEventType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SafeguardEventRepository extends JpaRepository<SafeguardEvent, UUID> {

    boolean existsBySessionIdAndEventId(UUID sessionId, UUID eventId);

    Optional<SafeguardEvent> findBySessionIdAndEventId(UUID sessionId, UUID eventId);

    Long countBySessionIdAndEventType(UUID sessionId, SafeguardEventType eventType);

    List<SafeguardEvent> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);
}