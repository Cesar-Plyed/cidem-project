package edu.cidemags.integrity_core.legal.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.BreachType;
import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import edu.cidemags.integrity_core.legal.domain.model.DPIAStatus;
import edu.cidemags.integrity_core.legal.domain.model.ObjectStatus;
import edu.cidemags.integrity_core.legal.domain.model.ObjectType;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionType;
import edu.cidemags.integrity_core.legal.domain.model.RiskLevel;

/** Cuerpos de peticion de la API legal. */
public final class LegalRequests {

    private LegalRequests() {
    }

    public record ConsentRequest(UUID studentId, ConsentType consentType) {
    }

    public record ConsentRevokeAllRequest(UUID studentId) {
    }

    public record ObjectRequestBody(UUID studentId, ObjectType objectType, String reason) {
    }

    public record ObjectResolveBody(ObjectStatus status, String resolutionNote) {
    }

    public record RestrictionBody(UUID studentId, RestrictionType restrictionType, String reason,
                                  Instant expiresAt) {
    }

    public record BreachBody(UUID studentId, BreachType breachType, String description) {
    }

    public record DpiaBody(String title, String description, RiskLevel riskLevel,
                           List<String> mitigationMeasures) {
    }

    public record DpiaReviewBody(DPIAStatus status, UUID reviewedBy) {
    }
}
