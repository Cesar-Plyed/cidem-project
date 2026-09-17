package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteExamUseCase {
    private final ExamRepository examRepository;

    public DeleteExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional
    public void execute(UUID examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException(examId));
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede eliminar un examen en borrador");
        }
        examRepository.deleteById(examId);
    }
}