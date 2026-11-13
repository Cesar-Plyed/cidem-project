package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Registro append-only del consentimiento de un alumno (specs 36, 40, 48).
 * Cada otorgamiento/revocacion inserta una fila nueva; nunca se actualiza.
 */
@Entity
@Table(name = "consent_logs")
public class ConsentLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30)
    private ConsentType consentType;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsentLog() {
    }

    private ConsentLog(UUID studentId, ConsentType consentType, Instant grantedAt, Instant revokedAt,
                       String ip, String userAgent) {
        this.studentId = studentId;
        this.consentType = consentType;
        this.grantedAt = grantedAt;
        this.revokedAt = revokedAt;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public static ConsentLog grant(UUID studentId, ConsentType consentType, String ip, String userAgent) {
        return new ConsentLog(studentId, consentType, Instant.now(), null, ip, userAgent);
    }

    public static ConsentLog revoke(UUID studentId, ConsentType consentType, String ip, String userAgent) {
        return new ConsentLog(studentId, consentType, null, Instant.now(), ip, userAgent);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Momento efectivo del ultimo cambio de estado. */
    public Instant effectiveAt() {
        if (grantedAt == null) {
            return revokedAt;
        }
        if (revokedAt == null) {
            return grantedAt;
        }
        return grantedAt.isAfter(revokedAt) ? grantedAt : revokedAt;
    }

    public boolean isGranted() {
        return grantedAt != null && (revokedAt == null || grantedAt.isAfter(revokedAt));
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public ConsentType getConsentType() {
        return consentType;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
