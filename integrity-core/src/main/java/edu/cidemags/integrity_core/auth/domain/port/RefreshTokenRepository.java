package edu.cidemags.integrity_core.auth.domain.port;

import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void revokeAllByUserId(UUID userId);
}