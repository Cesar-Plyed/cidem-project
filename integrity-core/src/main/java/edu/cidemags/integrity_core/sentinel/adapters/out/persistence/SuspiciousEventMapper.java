package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;

import java.time.Instant;

/**
 * Mapper manual entre {@link SuspiciousEvent} y {@link SuspiciousEventEntity}.
 * La entidad guarda {@code event_id} como texto (contrato del spec 06) y el
 * dominio usa el UUID del evento como identificador.
 */
public final class SuspiciousEventMapper {

    private static final String DEFAULT_SOURCE = "SENTINEL";

    private SuspiciousEventMapper() {
    }

    public static SuspiciousEventEntity toEntity(SuspiciousEvent event) {
        return new SuspiciousEventEntity(
            event.id(),
            event.id().toString(),
            event.examId(),
            event.examSessionId().value(),
            event.studentId().value(),
            event.attemptId().value(),
            event.type(),
            event.source() == null ? DEFAULT_SOURCE : event.source(),
            event.payload(),
            event.occurredAt(),
            Instant.now()
        );
    }

    public static SuspiciousEvent toDomain(SuspiciousEventEntity entity) {
        return new SuspiciousEvent(
            entity.getId(),
            new ExamSessionId(entity.getExamSessionId()),
            new StudentId(entity.getStudentId()),
            new AttemptId(entity.getAttemptId()),
            entity.getType(),
            entity.getOccurredAt(),
            entity.getExamId(),
            entity.getSource(),
            entity.getPayload()
        );
    }
}
