package edu.cidemags.integrity_core.evaluator.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;

/**
 * Vista del examen para el alumno que lo esta resolviendo. A proposito NO incluye
 * que opciones son correctas ni el texto de respuesta esperado.
 */
public record AttemptExamResponse(UUID attemptId, UUID examId, String title, String description,
        OffsetDateTime startedAt, int durationMinutes, boolean submitted, boolean integrityFailed,
        List<QuestionView> questions, List<AnswerView> answers) {

    public record QuestionView(UUID id, String prompt, QuestionType type, List<OptionView> options) {
    }

    public record OptionView(UUID id, String text) {
    }

    public record AnswerView(UUID questionId, List<UUID> selectedOptionIds, String openTextAnswer) {
    }
}
