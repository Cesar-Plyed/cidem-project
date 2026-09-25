package edu.cidemags.integrity_core.sentinel.adapters.in.websocket;

import java.time.Instant;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.Alert;


public record AlertNotification(
    AlertSeverity sverity,
    String reason,
    Instant generatedAt
) {

    public static AlertNotification from(Alert alert){
        return new AlertNotification(alert.severity(),alert.reason(), alert.generatedAt());
    }
} 