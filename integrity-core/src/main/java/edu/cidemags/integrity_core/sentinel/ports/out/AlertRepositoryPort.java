package edu.cidemags.integrity_core.sentinel.ports.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;

public interface AlertRepositoryPort {

    void save(Alert alert);

    List<Alert> findByExamSessionId(ExamSessionId examSessionId);

    /** Alertas de un examen, con filtros opcionales de severidad y fechas. */
    List<Alert> findByExamId(UUID examId, String severity, Instant from, Instant to);

    List<Alert> findByAttemptId(UUID attemptId, int limit);

    Optional<Alert> findById(UUID id);

    /** Marca la alerta como resuelta. Devuelve {@code false} si no existe. */
    boolean markResolved(UUID id);
}
