package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

public record ExamResultResponse(UUID attemptId, UUID examId, UUID studentId, int score, int maxScore,
        boolean pendingReview, List<QuestionResult> questions) {
    public record QuestionResult(UUID questionId, boolean answered, int score, boolean pendingReview) {
    }
}