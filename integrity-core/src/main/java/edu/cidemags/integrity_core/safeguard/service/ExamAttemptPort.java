package edu.cidemags.integrity_core.safeguard.service;

import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptPort {

    boolean examExists(UUID examId);

    Optional<AttemptRef> findAttempt(UUID attemptId);

    record AttemptRef(UUID attemptId, UUID examId, UUID studentId) {}
}
