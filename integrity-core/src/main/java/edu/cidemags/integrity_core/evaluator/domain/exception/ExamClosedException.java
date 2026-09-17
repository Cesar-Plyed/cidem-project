package edu.cidemags.integrity_core.evaluator.domain.exception;

import java.util.UUID;

public class ExamClosedException extends DomainException {
    public ExamClosedException(UUID examId) {
        super("El examen " + examId + " ya no acepta ingresos");
    }
}
