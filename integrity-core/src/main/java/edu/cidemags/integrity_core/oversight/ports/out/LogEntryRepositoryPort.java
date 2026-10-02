package edu.cidemags.integrity_core.oversight.ports.out;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;

import java.util.List;

public interface LogEntryRepositoryPort {

    void save(LogEntry entry);

    List<LogEntry> findByExamSessionId(ExamSessionId examSessionId);

    List<LogEntry> findByAttemptId(AttemptId attemptId);
}