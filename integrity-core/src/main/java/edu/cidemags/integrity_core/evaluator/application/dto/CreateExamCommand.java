package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

public record CreateExamCommand(String title, String description, UUID createdByUserId) {
}
