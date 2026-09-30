package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record ExamAverageResponse(
        UUID examId,
        String examTitle,
        int totalAttempts,
        int gradedAttempts,
        int pendingReviewCount,
        double averageScore,
        int maxScore,
        double averagePercentage) {
}
