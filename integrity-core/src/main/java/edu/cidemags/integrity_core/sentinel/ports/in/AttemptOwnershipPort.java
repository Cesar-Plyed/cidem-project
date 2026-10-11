package edu.cidemags.integrity_core.sentinel.ports.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Valida la propiedad del intento desde el lado de sentinel. La implementación
 * vive en safeguard/infrastructure (JdbcExamAttemptAdapter), reutilizando la
 * misma consulta que ya usa SafeGuard; evita duplicar el acceso a exam_attempts.
 */
public interface AttemptOwnershipPort {

    record AttemptOwner(UUID attemptId, UUID examId, UUID studentId) {}

    Optional<AttemptOwner> findOwner(UUID attemptId);
}
