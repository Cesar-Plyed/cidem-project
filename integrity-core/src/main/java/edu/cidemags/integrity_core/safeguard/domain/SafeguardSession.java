package edu.cidemags.integrity_core.safeguard.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "safeguard_session")
public class SafeguardSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private SafeguardConfiguration configuration;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafeguardSessionStatus status = SafeguardSessionStatus.PENDING;

    @Column(name = "risk_score", nullable = false)
    private int riskScore = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    private List<SafeguardEvent> events = new ArrayList<>();

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

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

    public UUID getId() { return id; }
    public SafeguardConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(SafeguardConfiguration configuration) { this.configuration = configuration; }
    public UUID getExamId() { return examId; }
    public void setExamId(UUID examId) { this.examId = examId; }
    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public SafeguardSessionStatus getStatus() { return status; }
    public void setStatus(SafeguardSessionStatus status) { this.status = status; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public List<SafeguardEvent> getEvents() { return events; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafeguardSession other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(getClass());
    }
}