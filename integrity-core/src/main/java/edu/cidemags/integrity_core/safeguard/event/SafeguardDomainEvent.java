package edu.cidemags.integrity_core.safeguard.event;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardSessionStatus;

public sealed interface SafeguardDomainEvent {

    UUID sessionId();
    Instant occurredAt();

    record SessionStarted(UUID sessionId, UUID examId, UUID attemptId, UUID studentId,
                          Instant occurredAt) implements SafeguardDomainEvent {}

    record RiskEventRecorded(UUID sessionId, UUID examId, UUID attemptId, UUID studentId,
                             UUID eventId, SafeguardEventType eventType, int severity,
                             int riskPoints, int totalRiskScore,
                             Instant occurredAt) implements SafeguardDomainEvent {}

    record SessionStatusChanged(UUID sessionId, UUID examId, UUID attemptId, UUID studentId,
                                SafeguardSessionStatus previousStatus, SafeguardSessionStatus newStatus,
                                int totalRiskScore, Instant occurredAt) implements SafeguardDomainEvent {}

    record SessionClosed(UUID sessionId, UUID examId, UUID attemptId, UUID studentId,
                         SafeguardSessionStatus finalStatus, int totalRiskScore,
                         Instant occurredAt) implements SafeguardDomainEvent {}
}