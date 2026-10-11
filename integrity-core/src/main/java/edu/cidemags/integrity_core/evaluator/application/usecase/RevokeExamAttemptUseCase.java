package edu.cidemags.integrity_core.evaluator.application.usecase;

import java.util.UUID;

import edu.cidemags.integrity_core.shared.events.ExamAttemptRevokedEvent;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokeExamAttemptUseCase {

    private final ExamAttemptRepository attemptRepository;
    private final ExamRepository examRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RevokeExamAttemptUseCase(ExamAttemptRepository attemptRepository, ExamRepository examRepository,
            ApplicationEventPublisher eventPublisher) {
        this.attemptRepository = attemptRepository;
        this.examRepository = examRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ExamAttempt execute(UUID attemptId, UUID requesterId, boolean administrator) {
        if (attemptId == null || requesterId == null) {
            throw new IllegalArgumentException("El intento y el usuario son obligatorios");
        }
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ExamNotFoundException(attemptId));
        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(attempt.getExamId()));
        if (!administrator && !exam.getCreatedByUserId().equals(requesterId)) {
            throw new AccessDeniedException("No puedes revocar intentos de otro profesor");
        }
        if (attempt.isIntegrityFailed()) {
            return attempt;
        }
        attempt.failForIntegrity();
        ExamAttempt revoked = attemptRepository.save(attempt);
        eventPublisher.publishEvent(new ExamAttemptRevokedEvent(
                exam.getId(), revoked.getId(), revoked.getStudentId(), java.time.Instant.now()));
        return revoked;
    }
}
