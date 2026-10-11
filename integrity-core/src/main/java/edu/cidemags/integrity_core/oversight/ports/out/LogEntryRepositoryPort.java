package edu.cidemags.integrity_core.oversight.ports.out;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;

import java.util.List;

public interface LogEntryRepositoryPort {

    void save(LogEntry entry);

    /** Guarda la entrada asociando la huella de dedup (puede ser null). */
    void save(LogEntry entry, String dedupKey);

    /** true si ya existe una entrada con la misma huella (dedup entre SafeGuard REST y Sentinel WS). */
    boolean existsByDedupKey(String dedupKey);

    List<LogEntry> findByExamSessionId(ExamSessionId examSessionId);

    List<LogEntry> findByAttemptId(AttemptId attemptId);
}