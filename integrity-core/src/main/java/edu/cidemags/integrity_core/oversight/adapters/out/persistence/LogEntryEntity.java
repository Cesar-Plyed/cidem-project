package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oversight_log_entries")
public class LogEntryEntity {

    @Id
    private UUID id;

    @Column(name = "exam_session_id", nullable = false)
    private UUID examSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private LogSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LogEntryEntity() {
    }

    public LogEntryEntity(UUID id, UUID examSessionId, UUID studentId, UUID attemptId, LogSource source,
                           AlertSeverity severity, String description, Instant occurredAt, Instant recordedAt) {
        this.id = id;
        this.examSessionId = examSessionId;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.source = source;
        this.severity = severity;
        this.description = description;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public UUID getExamSessionId() { return examSessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getAttemptId() { return attemptId; }
    public LogSource getSource() { return source; }
    public AlertSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getRecordedAt() { return recordedAt; }
}