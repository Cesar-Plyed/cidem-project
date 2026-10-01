package edu.cidemags.integrity_core.safeguard.domain.port;

import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;

public interface SafeguardEventRepository {

    Optional<SafeguardEvent> findBySessionIdAndEventId(UUID sessionId, UUID eventId);

    Long countBySessionIdAndEventType(UUID sessionId, SafeguardEventType eventType);

    SafeguardEvent saveAndFlush(SafeguardEvent event);
}
