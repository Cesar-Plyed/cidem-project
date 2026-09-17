package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record UpdateExamCommand(UUID examId, String title, String description) {
}