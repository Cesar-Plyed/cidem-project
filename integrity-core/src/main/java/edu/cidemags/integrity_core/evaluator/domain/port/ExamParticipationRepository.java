package edu.cidemags.integrity_core.evaluator.domain.port;

import java.util.UUID;

public interface ExamParticipationRepository {
    boolean reserve(UUID examId, UUID studentId);
}
