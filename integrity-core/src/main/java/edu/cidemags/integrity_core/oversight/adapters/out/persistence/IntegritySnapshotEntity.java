package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oversight_integrity_snapshots")
public class IntegritySnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "exam_session_id", nullable = false)
    private UUID examSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "score", nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    protected IntegritySnapshotEntity() {
    }

    public IntegritySnapshotEntity(UUID id, UUID examSessionId, UUID studentId, UUID attemptId,
                                    int score, RiskLevel riskLevel, Instant lastUpdatedAt) {
        this.id = id;
        this.examSessionId = examSessionId;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.score = score;
        this.riskLevel = riskLevel;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public UUID getId() { return id; }
    public UUID getExamSessionId() { return examSessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getAttemptId() { return attemptId; }
    public int getScore() { return score; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
}