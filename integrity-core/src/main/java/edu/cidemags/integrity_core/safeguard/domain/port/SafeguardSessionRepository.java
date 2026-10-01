package edu.cidemags.integrity_core.safeguard.domain.port;

import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;

public interface SafeguardSessionRepository {

    Optional<SafeguardSession> findByAttemptId(UUID attemptId);

    /** Bloqueo pesimista de la fila: serializa reintentos concurrentes sobre la misma sesión. */
    Optional<SafeguardSession> findByIdForUpdate(UUID id);

    SafeguardSession saveAndFlush(SafeguardSession session);
}
