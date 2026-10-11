package edu.cidemags.integrity_core.evaluator.domain.exception;

import java.util.UUID;

public class ExamAlreadyAttemptedException extends DomainException {
    public ExamAlreadyAttemptedException(UUID examId) {
        super("Ya realizaste este examen; solo se permite un intento por examen");
    }
}
