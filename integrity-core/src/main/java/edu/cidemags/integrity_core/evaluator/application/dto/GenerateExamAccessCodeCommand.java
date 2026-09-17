package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record GenerateExamAccessCodeCommand(UUID examId, String code, int expiresInMinutes) {
}
