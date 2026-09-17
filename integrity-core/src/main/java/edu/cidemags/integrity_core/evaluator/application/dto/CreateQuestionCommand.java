package edu.cidemags.integrity_core.evaluator.application.dto;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;

import java.util.List;

public record CreateQuestionCommand(String prompt, QuestionType type, List<QuestionOptionCommand> options,
        String correctAnswerText) {
    public record QuestionOptionCommand(String text, boolean correct) {
    }
}