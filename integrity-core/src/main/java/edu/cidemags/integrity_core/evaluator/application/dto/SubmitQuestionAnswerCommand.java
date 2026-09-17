package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

public record SubmitQuestionAnswerCommand(UUID attemptId, UUID questionId, List<UUID> selectedOptionIds,
        String openTextAnswer) {
}
