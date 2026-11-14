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

    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "exam_id")
    private UUID examId;

    @Column(name = "exam_session_id", nullable = false)
    private UUID examSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EventType type;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SuspiciousEventEntity() {
        // constructor vacio
    }

    public SuspiciousEventEntity(UUID id, String eventId, UUID examId, UUID examSessionId,
                                 UUID studentId, UUID attemptId, EventType type, String source,
                                 String payload, Instant occurredAt, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.examId = examId;
        this.examSessionId = examSessionId;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.type = type;
        this.source = source;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public UUID getExamId() { return examId; }
    public UUID getExamSessionId() { return examSessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getAttemptId() { return attemptId; }
    public EventType getType() { return type; }
    public String getSource() { return source; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
