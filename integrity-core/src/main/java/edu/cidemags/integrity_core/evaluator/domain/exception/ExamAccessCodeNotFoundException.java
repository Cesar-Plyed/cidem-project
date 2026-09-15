package edu.cidemags.integrity_core.evaluator.domain.exception;

import java.util.UUID;

public class ExamAccessCodeNotFoundException extends DomainException {
    public ExamAccessCodeNotFoundException(UUID examId, String code) {
        super("Codigo de acceso invalido para el examen " + examId + ": " + code);
    }
}
