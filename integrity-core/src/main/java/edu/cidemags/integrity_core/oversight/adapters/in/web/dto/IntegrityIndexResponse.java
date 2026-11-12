package edu.cidemags.integrity_core.oversight.adapters.in.web.dto;

import edu.cidemags.integrity_core.oversight.domain.RiskLevel;
import edu.cidemags.integrity_core.oversight.domain.service.IntegrityIndexProperties;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * Response for GET /oversight/attempts/{attemptId}/integrity-index.
 *
 * @param score     score de integridad (0-100)
 * @param riskLevel nivel de riesgo
 * @param reason      razón del cálculo
 * @param breakdown   desglose por tipo de evento
 */
public record IntegrityIndexResponse(
        BigDecimal score,
        RiskLevel riskLevel,
        String reason,
        Map<String, Long> breakdown
) {

    public static IntegrityIndexResponse from(
            BigDecimal score,
            String reason,
            Map<String, Long> breakdown
    ) {
        RiskLevel riskLevel = IntegrityIndexProperties.toRiskLevel(score);
        return new IntegrityIndexResponse(score, riskLevel, reason, Collections.unmodifiableMap(breakdown));
    }
}
