package edu.cidemags.integrity_core.oversight.adapters.in.event;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;

public record OversightRealtimeEvent(
        UUID examId,
        UUID attemptId,
        UUID studentId,
        LogSource source,
        AlertSeverity severity,
        String description,
        Instant occurredAt) {
}
