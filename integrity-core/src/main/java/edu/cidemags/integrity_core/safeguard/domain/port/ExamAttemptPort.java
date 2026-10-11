package edu.cidemags.integrity_core.safeguard.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptPort {

    boolean examExists(UUID examId);

    Optional<AttemptRef> findAttempt(UUID attemptId);

    /** TRUE si el alumno tiene al menos un intento en el examen (lectura de config por ALUMNO). */
    boolean attemptExistsForStudent(UUID examId, UUID studentId);

    void failAttemptForIntegrity(UUID attemptId);

    record AttemptRef(UUID attemptId, UUID examId, UUID studentId) {}
}
