package edu.cidemags.integrity_core.oversight.adapters.in.web;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;

import java.time.Instant;
import java.util.UUID;

public record LogEntryResponse(
    UUID studentId,
    UUID attemptId,
    LogSource source,
    AlertSeverity severity,
    String description,
    Instant occurredAt
) {
    public static LogEntryResponse from(LogEntry entry) {
        return new LogEntryResponse(
            entry.studentId().value(),
            entry.attemptId().value(),
            entry.source(),
            entry.severity(),
            entry.description(),
            entry.occurredAt()
        );
    }
}