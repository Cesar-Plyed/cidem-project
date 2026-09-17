package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record UpdateQuestionBankCommand(UUID bankId, String name, String description) {
}