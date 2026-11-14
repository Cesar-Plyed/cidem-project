package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.BreachStatus;
import edu.cidemags.integrity_core.legal.domain.model.BreachType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Notificacion al interesado de una brecha de datos (Art. 34 GDPR, spec 49). */
@Entity
@Table(name = "data_breach_notifications")
public class DataBreachNotification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "breach_type", nullable = false, length = 30)
    private BreachType breachType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "notified_at", nullable = false)
    private Instant notifiedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private BreachStatus status = BreachStatus.PENDING;

    protected DataBreachNotification() {
    }

    public DataBreachNotification(UUID studentId, BreachType breachType, String description,
                                  String recommendedAction) {
        this.studentId = studentId;
        this.breachType = breachType;
        this.description = description;
        this.recommendedAction = recommendedAction;
    }

    public void markNotified() {
        this.status = BreachStatus.NOTIFIED;
    }

    public void resolve() {
        this.status = BreachStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public BreachType getBreachType() {
        return breachType;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public BreachStatus getStatus() {
        return status;
    }
}
