package edu.cidemags.integrity_core.evaluator.domain.exception;

import java.util.UUID;

public class ExamNotFoundException extends DomainException {
    public ExamNotFoundException(UUID examId) {
        super("No existe el examen con id: " + examId);
    }
}
