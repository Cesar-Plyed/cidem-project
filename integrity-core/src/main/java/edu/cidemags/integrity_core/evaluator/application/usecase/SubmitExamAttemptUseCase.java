package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.SubmitExamAttemptCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitExamAttemptUseCase {

    private final ExamAttemptRepository examAttemptRepository;

    public SubmitExamAttemptUseCase(ExamAttemptRepository examAttemptRepository) {
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional
    public ExamAttempt execute(SubmitExamAttemptCommand command) {
        if (command == null || command.attemptId() == null) {
            throw new IllegalArgumentException("El identificador de la entrega es obligatorio");
        }

        ExamAttempt attempt = examAttemptRepository.findById(command.attemptId())
                .orElseThrow(() -> new ExamNotFoundException(command.attemptId()));

        attempt.submit();
        return examAttemptRepository.save(attempt);
    }
}
