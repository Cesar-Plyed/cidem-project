package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateExamUseCase {

    private final ExamRepository examRepository;

    public CreateExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional
    public Exam execute(CreateExamCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        Exam exam = new Exam(command.title(), command.description(), command.createdByUserId());
        return examRepository.save(exam);
    }
}
