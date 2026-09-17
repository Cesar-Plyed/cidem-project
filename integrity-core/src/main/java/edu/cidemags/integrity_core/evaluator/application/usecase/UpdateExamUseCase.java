package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.UpdateExamCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateExamUseCase {
    private final ExamRepository examRepository;

    public UpdateExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional
    public Exam execute(UpdateExamCommand command) {
        if (command == null || command.examId() == null) {
            throw new IllegalArgumentException("El identificador del examen es obligatorio");
        }
        Exam exam = examRepository.findById(command.examId())
                .orElseThrow(() -> new ExamNotFoundException(command.examId()));
        exam.updateDetails(command.title(), command.description());
        return examRepository.save(exam);
    }
}