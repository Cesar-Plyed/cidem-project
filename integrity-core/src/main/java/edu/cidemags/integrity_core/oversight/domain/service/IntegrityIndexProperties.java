package edu.cidemags.integrity_core.oversight.domain.service;

import edu.cidemags.integrity_core.oversight.domain.RiskLevel;

import java.math.BigDecimal;

/**
 * Propiedades configurables para el cálculo del índice de integridad.
 *
 * <p>Se puede inyectar con @ConfigurationProperties(prefix = "integrity.index") para
 * personalizar pesos, score base y nivel de riesgo.</p>
 *
 * @param baseScore              score base (por defecto 100)
 * @param tabSwitchWeight        penalización por TAB_SWITCH (por defecto 30)
 * @param focusLossWeight        penalización por FOCUS_LOSS_REPEAT (por defecto 15)
 * @param safeguardBlockWeight   penalización por SAFEGUARD_BLOCK (por defecto 25)
 * @param prolongedDisconnectWeight penalización por PROLONGED_DISCONNECT (por defecto 10)
 * @param riskThreshold          threshold para riesgo medio (por defecto 60)
 */
public record IntegrityIndexProperties(
        BigDecimal baseScore,
        BigDecimal tabSwitchWeight,
        BigDecimal focusLossWeight,
        BigDecimal safeguardBlockWeight,
        BigDecimal prolongedDisconnectWeight,
        BigDecimal riskThreshold
) {

    public static IntegrityIndexProperties defaults() {
        return new IntegrityIndexProperties(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(60)
        );
    }

    /**
     * Convierte un BigDecimal score a RiskLevel.
     */
    public static RiskLevel toRiskLevel(BigDecimal score, BigDecimal riskThreshold) {
        if (score.compareTo(riskThreshold) < 0) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.MEDIUM;
    }

    public static RiskLevel toRiskLevel(BigDecimal score) {
        return toRiskLevel(score, defaults().riskThreshold);
    }
}
