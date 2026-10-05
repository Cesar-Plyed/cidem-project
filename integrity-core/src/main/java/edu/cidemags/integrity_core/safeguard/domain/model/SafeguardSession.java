package edu.cidemags.integrity_core.safeguard.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio puro: sin anotaciones de JPA ni de framework.
 * La representación persistida vive en infrastructure/persistence/entity.
 */
public class SafeguardSession {

    private UUID id;
    private SafeguardConfiguration configuration;
    private UUID examId;
    private UUID attemptId;
    private UUID studentId;
    private SafeguardSessionStatus status = SafeguardSessionStatus.PENDING;
    private int riskScore = 0;
    private Instant startedAt;
    private Instant endedAt;
    private String clientIp;
    private String userAgent;
    private Instant lastHeartbeatAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
