package edu.cidemags.integrity_core.evaluator.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentExamHistoryResponse(
        UUID attemptId,
        UUID examId,
        String examTitle,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt,
        int score,
        int maxScore,
        boolean pendingReview,
        boolean integrityFailed) {
}
