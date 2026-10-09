package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.SubmitExamAttemptCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubmitExamAttemptUseCase {

    private final ExamAttemptRepository examAttemptRepository;

    public SubmitExamAttemptUseCase(ExamAttemptRepository examAttemptRepository) {
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional
    public ExamAttempt execute(SubmitExamAttemptCommand command, UUID requesterId) {
        if (command == null || command.attemptId() == null || requesterId == null) {
            throw new IllegalArgumentException("El identificador de la entrega y el usuario son obligatorios");
        }

        ExamAttempt attempt = examAttemptRepository.findById(command.attemptId())
                .orElseThrow(() -> new ExamNotFoundException(command.attemptId()));
        if (!attempt.getStudentId().equals(requesterId)) {
            throw new AccessDeniedException("No puedes enviar el intento de otro estudiante");
        }

        attempt.submit();
        return examAttemptRepository.save(attempt);
    }
}
