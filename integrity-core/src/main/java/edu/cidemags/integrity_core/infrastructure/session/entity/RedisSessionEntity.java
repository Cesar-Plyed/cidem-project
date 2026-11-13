package edu.cidemags.integrity_core.infrastructure.session.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para sesiones de examen persistidas en PostgreSQL.
 * <p>
 * Esta tabla almacena el estado persistente de cada sesión;
 * {@link edu.cidemags.integrity_core.infrastructure.session.RedisSessionService}
 * mantiene una copia en Redis con TTL.
 * </p>
 */
@Entity
@Table(name = "exam_session")
public class RedisSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID examId;

    @Column(nullable = false)
    private UUID attemptId;

    @Column(nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Column(nullable = false)
    private long maxDurationSeconds;

    @Column(length = 1000)
    private String payload;

    public enum SessionStatus {
        RUNNING,
        PAUSED,
        COMPLETED,
        BLOCKED,
        EXPIRED
    }

    public RedisSessionEntity() {}

    public RedisSessionEntity(UUID examId, UUID attemptId, UUID studentId, SessionStatus status, Instant startedAt, long maxDurationSeconds) {
        this.examId = examId;
        this.attemptId = attemptId;
        this.studentId = studentId;
        this.status = status;
        this.startedAt = startedAt;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public UUID getId() { return id; }
    public UUID getExamId() { return examId; }
    public UUID getAttemptId() { return attemptId; }
    public UUID getStudentId() { return studentId; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public long getMaxDurationSeconds() { return maxDurationSeconds; }
    public String getPayload() { return payload; }

    public boolean isFinished() {
        return status == SessionStatus.COMPLETED || status == SessionStatus.BLOCKED || status == SessionStatus.EXPIRED;
    }
}
