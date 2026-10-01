package edu.cidemags.integrity_core.safeguard.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio puro: sin anotaciones de JPA ni de framework.
 * La representación persistida vive en infrastructure/persistence/entity.
 */
public class SafeguardEvent {

    private UUID id;
    private SafeguardSession session;
    private UUID eventId;
    private SafeguardEventType eventType;
    private int severity = 1;
    private int riskPoints = 0;
    private Instant occurredAt;
    private String metadata;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SafeguardSession getSession() { return session; }
    public void setSession(SafeguardSession session) { this.session = session; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
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
