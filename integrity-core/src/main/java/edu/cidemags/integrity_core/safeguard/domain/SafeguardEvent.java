package edu.cidemags.integrity_core.safeguard.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "safeguard_event")
public class SafeguardEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SafeguardSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private SafeguardEventType eventType;

    @Column(name = "severity", nullable = false)
    private int severity = 1;

    @Column(name = "risk_points", nullable = false)
    private int riskPoints = 0;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "event_id", nullable = false, updatable = false)
    private java.util.UUID eventId;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.occurredAt == null) {
            this.occurredAt = this.createdAt;
        }
    }

    public UUID getId() { return id; }
    public SafeguardSession getSession() { return session; }
    public void setSession(SafeguardSession session) { this.session = session; }
    public SafeguardEventType getEventType() { return eventType; }
    public void setEventType(SafeguardEventType eventType) { this.eventType = eventType; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public int getRiskPoints() { return riskPoints; }
    public void setRiskPoints(int riskPoints) { this.riskPoints = riskPoints; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID enevtId) { this.eventId = enevtId; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafeguardEvent other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(getClass());
    }
}