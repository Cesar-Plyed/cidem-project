package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteExamUseCase {
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public DeleteExamUseCase(ExamRepository examRepository, ExamAttemptRepository examAttemptRepository) {
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional
    public void execute(UUID examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException(examId));
        if (examAttemptRepository.existsByExamId(examId)) {
            throw new IllegalStateException(
                    "No se puede eliminar el examen porque ya tiene intentos de alumnos");
        }
        examRepository.deleteById(examId);
    }
}