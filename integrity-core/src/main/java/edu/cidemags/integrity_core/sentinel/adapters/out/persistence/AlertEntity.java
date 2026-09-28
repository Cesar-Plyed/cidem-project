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

    protected AlertEntity() {
        // constructor vacio
    }

    public AlertEntity(UUID id, UUID examSessionId, UUID studentId, UUID attemptId, AlertSeverity severity,
                       String reason, List<UUID> sourceEventIds, Instant generatedAt) {
        this.id = id;
        this.examSessionId = examSessionId;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.severity = severity;
        this.reason = reason;
        this.sourceEventIds = sourceEventIds;
        this.generatedAt = generatedAt;
    }

    public UUID getId() { return id; }
    public UUID getExamSessionId() { return examSessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getAttemptId() { return attemptId; }
    public AlertSeverity getSeverity() { return severity; }
    public String getReason() { return reason; }
    public List<UUID> getSourceEventIds() { return sourceEventIds; }
    public Instant getGeneratedAt() { return generatedAt; }
}