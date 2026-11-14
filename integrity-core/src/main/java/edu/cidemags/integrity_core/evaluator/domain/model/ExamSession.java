package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Modelo de dominio para una sesión de examen en curso.
 * <p>
 * Representa una instancia de un examen que un alumno está respondiendo
 * en ese momento (estado RUNNING/PAUSED). Se serializa a Redis con TTL.
 * </p>
 *
 * @param id           Identificador único de la sesión
 * @param examId       ID del examen
 * @param attemptId    ID del intento de examen
 * @param studentId    ID del alumno
 * @param status       Estado actual (RUNNING, PAUSED, COMPLETED, BLOCKED, EXPIRED)
 * @param startedAt    Momento de inicio
 * @param maxDuration  Duración máxima en segundos
 */
public record ExamSession(
        UUID id,
        UUID examId,
        UUID attemptId,
        UUID studentId,
        SessionStatus status,
        Instant startedAt,
        long maxDuration) {

    public enum SessionStatus {
        RUNNING,
        PAUSED,
        COMPLETED,
        BLOCKED,
        EXPIRED
    }

    public boolean finished() {
        return status == SessionStatus.COMPLETED
                || status == SessionStatus.BLOCKED
                || status == SessionStatus.EXPIRED;
    }

    public static ExamSession from(String id, String examId, String attemptId, String status, String startedAt, long maxDuration) {
        return new ExamSession(
                UUID.fromString(id),
                UUID.fromString(examId),
                UUID.fromString(attemptId),
                UUID.randomUUID(),
                SessionStatus.valueOf(status),
                Instant.parse(startedAt),
                maxDuration
        );
    }
}
