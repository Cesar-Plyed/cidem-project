package edu.cidemags.integrity_core.evaluator.domain.exception;

import java.util.UUID;

public class QuestionBankNotFoundException extends DomainException {
    public QuestionBankNotFoundException(UUID bankId) {
        super("No existe el banco de preguntas con id: " + bankId);
    }
}