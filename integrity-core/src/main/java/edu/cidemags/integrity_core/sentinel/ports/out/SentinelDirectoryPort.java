package edu.cidemags.integrity_core.sentinel.ports.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Lecturas de solo lectura que Sentinel necesita para enriquecer sus
 * respuestas (nombres) y resolver el examen de una sesion. Las tablas
 * consultadas pertenecen a otros modulos (auth/evaluator); Sentinel solo lee.
 */
public interface SentinelDirectoryPort {

    Optional<String> findStudentName(UUID studentId);

    Optional<String> findExamName(UUID examId);

    /** Primer examen asociado a una sesion (alertas o eventos), si existe. */
    Optional<UUID> findExamIdBySession(UUID examSessionId);
}
