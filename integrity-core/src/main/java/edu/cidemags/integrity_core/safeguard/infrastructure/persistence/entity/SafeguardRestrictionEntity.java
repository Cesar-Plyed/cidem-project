package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionSeverity;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;

@Entity
@Table(name = "safeguard_restriction")
public class SafeguardRestrictionEntity {

    @Id
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private RestrictionType type;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "max_occurrences", nullable = false)
    private int maxOccurrences = 3;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds = 300;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private RestrictionSeverity severity = RestrictionSeverity.MEDIUM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public RestrictionType getType() {
        return type;
    }

    public void setType(RestrictionType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public void setMaxOccurrences(int maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public RestrictionSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RestrictionSeverity severity) {
        this.severity = severity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
