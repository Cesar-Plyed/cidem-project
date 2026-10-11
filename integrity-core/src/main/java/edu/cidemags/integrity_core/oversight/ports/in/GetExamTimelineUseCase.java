package edu.cidemags.integrity_core.oversight.ports.in;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;

import java.util.List;
import java.util.UUID;

public interface GetExamTimelineUseCase {
    List<LogEntry> getTimeline(UUID examSessionId);
}