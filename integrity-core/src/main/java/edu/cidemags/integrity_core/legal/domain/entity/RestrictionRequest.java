package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.RestrictionStatus;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Limitacion del tratamiento (Art. 18 GDPR, spec 47). */
@Entity
@Table(name = "restriction_requests")
public class RestrictionRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "restriction_type", nullable = false, length = 20)
    private RestrictionType restrictionType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private RestrictionStatus status = RestrictionStatus.PENDING;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected RestrictionRequest() {
    }

    public RestrictionRequest(UUID studentId, RestrictionType restrictionType, String reason, Instant expiresAt) {
        this.studentId = studentId;
        this.restrictionType = restrictionType;
        this.reason = reason;
        this.expiresAt = expiresAt;
        // La restriccion se aplica en cuanto se solicita; queda ACTIVE hasta
        // revocarse o expirar (Art. 18.1 GDPR).
        this.status = RestrictionStatus.ACTIVE;
    }

    public void revoke() {
        this.status = RestrictionStatus.REVOKED;
    }

    public boolean isActive() {
        return status == RestrictionStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public RestrictionType getRestrictionType() {
        return restrictionType;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public RestrictionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
