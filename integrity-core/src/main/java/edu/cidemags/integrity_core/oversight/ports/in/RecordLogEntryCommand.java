package edu.cidemags.integrity_core.oversight.ports.in;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;

import java.time.Instant;
import java.util.UUID;

public record RecordLogEntryCommand(
    UUID examSessionId,
    UUID studentId,
    UUID attemptId,
    LogSource source,
    AlertSeverity severity,
    String description,
    Instant occurredAt
) {
}