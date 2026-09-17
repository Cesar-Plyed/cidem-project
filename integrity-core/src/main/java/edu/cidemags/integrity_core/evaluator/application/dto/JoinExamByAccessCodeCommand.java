package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record JoinExamByAccessCodeCommand(UUID examId, String code, UUID studentId) {
}
