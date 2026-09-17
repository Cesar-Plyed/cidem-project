package edu.cidemags.integrity_core.auth.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class RefreshToken {

    private UUID id;
    private final UUID userId;
    private final String token;
    private final OffsetDateTime expiresAt;
    private boolean revoked;

    public RefreshToken(UUID userId, String token, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public RefreshToken(UUID id, UUID userId, String token, OffsetDateTime expiresAt, boolean revoked) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public boolean isExpired(){
        return OffsetDateTime.now(ZoneId.systemDefault()).isAfter(expiresAt);
    }

    public boolean isValid(){
        return !revoked && !isExpired();
    }

    public void revoke(){
        this.revoked = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
