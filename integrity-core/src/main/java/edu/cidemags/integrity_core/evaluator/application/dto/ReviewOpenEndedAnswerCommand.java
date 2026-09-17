package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record ReviewOpenEndedAnswerCommand(UUID answerId, UUID reviewerId, int score, String feedback) {
}
