package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sentinel_alerts")
public class AlertEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "exam_id")
    private UUID examId;

    @Column(name = "exam_session_id", nullable = false)
    private UUID examSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Column(name = "reason", nullable = false)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_event_ids", columnDefinition = "jsonb", nullable = false)
    private List<UUID> sourceEventIds;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AlertEntity() {
        // constructor vacio
    }

    public AlertEntity(UUID id, String eventId, String alertType, UUID examId, UUID examSessionId,
                       UUID studentId, UUID attemptId, AlertSeverity severity, String reason,
                       List<UUID> sourceEventIds, Instant generatedAt, boolean resolved, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.alertType = alertType;
        this.examId = examId;
        this.examSessionId = examSessionId;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.severity = severity;
        this.reason = reason;
        this.sourceEventIds = sourceEventIds;
        this.generatedAt = generatedAt;
        this.resolved = resolved;
        this.createdAt = createdAt;
    }

    public void markResolved() {
        this.resolved = true;
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAlertType() { return alertType; }
    public UUID getExamId() { return examId; }
    public UUID getExamSessionId() { return examSessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getAttemptId() { return attemptId; }
    public AlertSeverity getSeverity() { return severity; }
    public String getReason() { return reason; }
    public List<UUID> getSourceEventIds() { return sourceEventIds; }
    public Instant getGeneratedAt() { return generatedAt; }
    public boolean isResolved() { return resolved; }
    public Instant getCreatedAt() { return createdAt; }
}
