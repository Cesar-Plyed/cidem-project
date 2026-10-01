package edu.cidemags.integrity_core.safeguard.application.dto;

import java.util.Objects;
import java.util.UUID;

public record StartSessionCommand(UUID examId, UUID attemptId, UUID studentId,
                                  String clientIp, String userAgent) {
    public StartSessionCommand {
        Objects.requireNonNull(examId, "examId");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(studentId, "studentId");
    }
}
