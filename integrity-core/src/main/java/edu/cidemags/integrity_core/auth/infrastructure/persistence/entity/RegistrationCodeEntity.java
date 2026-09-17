package edu.cidemags.integrity_core.auth.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_codes")
public class RegistrationCodeEntity {
    
    @Id 
    @GeneratedValue 
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column
    private Role role;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RegistrationCodeEntity(){}

    public RegistrationCodeEntity(UUID id, String code, Role role, boolean used, OffsetDateTime expiresAt,
            OffsetDateTime usedAt) {
        this.id = id;
        this.code = code;
        this.role = role;
        this.used = used;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Role getRole() {
        return role;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
