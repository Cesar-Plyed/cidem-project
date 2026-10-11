package edu.cidemags.integrity_core.evaluator.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OpenEndedReviewResponse(
        UUID answerId,
        UUID attemptId,
        UUID studentId,
        UUID questionId,
        String questionPrompt,
        String openTextAnswer,
        Integer questionPoints,
        Integer score,
        String feedback,
        UUID reviewerId,
        boolean reviewed,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
