package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;

public record ExamResultResponse(UUID attemptId, UUID examId, UUID studentId, String studentEmail,
        String studentFirstName, String studentLastName, String studentPhone,
        int score, int maxScore, boolean pendingReview, List<QuestionResult> questions) {
    public record QuestionResult(UUID questionId, String statement, QuestionType questionType,
            List<UUID> selectedOptionIds, String openTextAnswer,
            List<OptionDetail> options,
            boolean answered, int score, boolean pendingReview, String feedback) {
        public record OptionDetail(UUID id, String text, boolean correct) {
        }
    }
}