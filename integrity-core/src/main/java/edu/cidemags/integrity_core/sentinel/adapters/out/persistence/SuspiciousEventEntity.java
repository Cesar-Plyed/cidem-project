package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sentinel_suspicious_events")
public class SuspiciousEventEntity {

    @Id
    private UUID id;

    @Column(name = "exam_session_id", nullable = false)
    private UUID examSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EventType type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected SuspiciousEventEntity() {
        // constructor vacio
    }

    public SuspiciousEventEntity(UUID id, UUID examSessionId, UUID studentId, UUID attemptId,
                                  EventType type, Instant occurredAt) {
        this.id = id;
        this.examSessionId = examSessionId;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.type = type;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getExamSessionId() { return examSessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getAttemptId() { return attemptId; }
    public EventType getType() { return type; }
    public Instant getOccurredAt() { return occurredAt; }
}