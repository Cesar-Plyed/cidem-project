package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

public record CreateExamCommand(String title, String description, UUID createdByUserId,
		List<CreateQuestionCommand> questions) {
	public CreateExamCommand(String title, String description, UUID createdByUserId) {
		this(title, description, createdByUserId, List.of());
	}
}
