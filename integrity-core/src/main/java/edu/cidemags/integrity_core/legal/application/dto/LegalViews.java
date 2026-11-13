package edu.cidemags.integrity_core.legal.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vistas de respuesta de la API legal. */
public final class LegalViews {

    private LegalViews() {
    }

    public record ConsentStatus(UUID id, String type, String status,
                                Instant grantedAt, Instant revokedAt) {
    }

    public record ConsentOverview(UUID studentId, List<ConsentStatus> consents) {
    }

    public record ConsentHistoryEntry(String type, String action, Instant timestamp) {
    }

    public record ConsentHistory(UUID studentId, List<ConsentHistoryEntry> history) {
    }

    public record RetentionInfo(int retentionYears, List<String> dataTypes, Instant nextDeletionDate) {
    }

    public record DataTypesInfo(List<String> required, List<String> optional,
                                Map<String, Integer> retentionDays) {
    }

    public record ObjectStatusView(UUID objectId, boolean objectActive, String objectType,
                                   String reason, Instant requestedAt, String status) {
    }

    public record RestrictionStatusView(UUID restrictionId, boolean restricted,
                                        String restrictionType, String reason,
                                        Instant requestedAt, Instant expiresAt, String status) {
    }

    public record BreachNotice(UUID id, String breachType, String description,
                               String recommendedAction, Instant notifiedAt, String status) {
    }

    public record BreachNotificationList(List<BreachNotice> notifications) {
    }

    public record DpiaSummary(UUID id, String title, String riskLevel, String status) {
    }

    public record DpiaList(List<DpiaSummary> dpias) {
    }
}
