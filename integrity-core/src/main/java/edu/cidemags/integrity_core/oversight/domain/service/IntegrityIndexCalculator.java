package edu.cidemags.integrity_core.oversight.domain.service;

import edu.cidemags.integrity_core.oversight.adapters.in.web.dto.IntegrityIndexResponse;
import edu.cidemags.integrity_core.oversight.domain.RiskLevel;
import edu.cidemags.integrity_core.oversight.domain.service.IntegrityIndexProperties;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.oversight.domain.LogEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Calcula el índice inicial de integridad basado en eventos de seguridad.
 *
 * <p>Fórmula: score = max(0, 100 - sum(peso * count for each event type))</p>
 *
 * <p>Pesos por defecto:</p>
 * <ul>
 *   <li>TAB_SWITCH: -30</li>
 *   <li>FOCUS_LOSS_REPEAT: -15</li>
 *   <li>SAFEGUARD_BLOCK: -25</li>
 *   <li>PROLONGED_DISCONNECT: -10</li>
 * </ul>
 */
public class IntegrityIndexCalculator {

    private final IntegrityIndexProperties properties;

    public IntegrityIndexCalculator(IntegrityIndexProperties properties) {
        this.properties = properties;
    }

    /**
     * Calcula el índice de integridad para un intento.
     *
     * @param suspiciousEvents eventos de tab switch y focus loss
     * @param alerts alertas de Sentinel
     * @param logEntries entradas de bitácora de SafeGuard + disconnect
     * @return IntegrityIndexResponse con score, reason y breakdown
     */
    public IntegrityIndexResponse calculate(
            List<SuspiciousEvent> suspiciousEvents,
            List<Alert> alerts,
            List<LogEntry> logEntries
    ) {
        // Count events by type
        Map<String, Long> breakdown = new LinkedHashMap<>();

        // TAB_SWITCH events
        long tabSwitchCount = suspiciousEvents.stream()
                .filter(e -> e.type().name().equals("TAB_SWITCH"))
                .count();
        breakdown.put("TAB_SWITCH", tabSwitchCount);

        // FOCUS_LOSS_REPEAT events
        long focusLossCount = suspiciousEvents.stream()
                .filter(e -> e.type().name().equals("FOCUS_LOSS_REPEAT"))
                .count();
        breakdown.put("FOCUS_LOSS_REPEAT", focusLossCount);

        // SAFEGUARD_BLOCK events (from log entries)
        long safeguardBlockCount = logEntries.stream()
                .filter(e -> "SAFEGUARD".equals(e.source().name()) && "SAFEGUARD_BLOCK".equals(e.description()))
                .count();
        breakdown.put("SAFEGUARD_BLOCK", safeguardBlockCount);

        // PROLONGED_DISCONNECT events (from log entries)
        long disconnectCount = logEntries.stream()
                .filter(e -> "SAFEGUARD".equals(e.source().name()) && "PROLONGED_DISCONNECT".equals(e.description()))
                .count();
        breakdown.put("PROLONGED_DISCONNECT", disconnectCount);

        // Calculate score
        BigDecimal penalty = BigDecimal.ZERO
                .add(properties.tabSwitchWeight().multiply(BigDecimal.valueOf(tabSwitchCount)))
                .add(properties.focusLossWeight().multiply(BigDecimal.valueOf(focusLossCount)))
                .add(properties.safeguardBlockWeight().multiply(BigDecimal.valueOf(safeguardBlockCount)))
                .add(properties.prolongedDisconnectWeight().multiply(BigDecimal.valueOf(disconnectCount)));

        BigDecimal score = properties.baseScore()
                .subtract(penalty)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // Build reason string
        String reason = buildReason(breakdown, tabSwitchCount, focusLossCount, safeguardBlockCount, disconnectCount);

        return IntegrityIndexResponse.from(score, reason, Collections.unmodifiableMap(breakdown));
    }

    private String buildReason(
            Map<String, Long> breakdown,
            long tabSwitchCount,
            long focusLossCount,
            long safeguardBlockCount,
            long disconnectCount
    ) {
        List<String> parts = new ArrayList<>();

        parts.add("Tab switch: " + tabSwitchCount);
        parts.add("Focus loss: " + focusLossCount);
        parts.add("Safeguard: " + safeguardBlockCount);
        parts.add("Disconnect: " + disconnectCount);

        return String.join(", ", parts);
    }
}
