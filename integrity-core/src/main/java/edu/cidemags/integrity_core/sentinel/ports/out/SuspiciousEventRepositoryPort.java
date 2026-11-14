package edu.cidemags.integrity_core.sentinel.ports.out;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;

public interface SuspiciousEventRepositoryPort {

    void save(SuspiciousEvent event);

    List<SuspiciousEvent> findByExamSessionId(ExamSessionId examSessionId);

    /** Eventos de un intento, ordenados del mas reciente al mas antiguo. */
    List<SuspiciousEvent> findByAttemptId(UUID attemptId, int limit);
}
