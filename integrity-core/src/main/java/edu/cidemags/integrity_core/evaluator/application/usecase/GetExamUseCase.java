package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetExamUseCase {
    private final ExamRepository examRepository;

    public GetExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public Exam execute(UUID examId) {
        if (examId == null) {
            throw new IllegalArgumentException("El identificador del examen es obligatorio");
        }
        return examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException(examId));
    }
}