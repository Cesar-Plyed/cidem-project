package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.SubmitQuestionAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubmitQuestionAnswerUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ExamRepository examRepository;

    public SubmitQuestionAnswerUseCase(ExamAttemptRepository examAttemptRepository,
            ExamAnswerRepository examAnswerRepository, ExamRepository examRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.examAnswerRepository = examAnswerRepository;
        this.examRepository = examRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = "exam-attempt-results", allEntries = true)
    public ExamAnswer execute(SubmitQuestionAnswerCommand command, UUID requesterId) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        if (command.attemptId() == null || command.questionId() == null || requesterId == null) {
            throw new IllegalArgumentException("El intento, la pregunta y el usuario son obligatorios");
        }

        ExamAttempt attempt = examAttemptRepository.findById(command.attemptId())
                .orElseThrow(() -> new ExamNotFoundException(command.attemptId()));
        if (!attempt.getStudentId().equals(requesterId)) {
            throw new AccessDeniedException("No puedes responder el intento de otro estudiante");
        }

        if (attempt.isSubmitted()) {
            throw new IllegalStateException("La entrega ya fue enviada");
        }

        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(attempt.getExamId()));
        Question question = exam.getQuestions().stream()
                .filter(candidate -> candidate.getId().equals(command.questionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La pregunta no pertenece al examen"));

        if (question.getType() == QuestionType.OPEN_ENDED
                && command.selectedOptionIds() != null && !command.selectedOptionIds().isEmpty()) {
            throw new IllegalArgumentException("Una pregunta abierta no admite opciones");
        }
        if (question.getType() != QuestionType.OPEN_ENDED
                && command.openTextAnswer() != null && !command.openTextAnswer().isBlank()) {
            throw new IllegalArgumentException("Una pregunta cerrada no admite texto libre");
        }
        if (command.selectedOptionIds() != null && command.selectedOptionIds().stream()
                .anyMatch(optionId -> question.getOptions().stream()
                        .noneMatch(option -> option.getId().equals(optionId)))) {
            throw new IllegalArgumentException("Una opcion no pertenece a la pregunta");
        }

        ExamAnswer answer = examAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), command.questionId())
                .map(existing -> new ExamAnswer(existing.getId(), attempt.getId(), command.questionId(),
                        command.selectedOptionIds(), command.openTextAnswer()))
                .orElseGet(() -> new ExamAnswer(UUID.randomUUID(), attempt.getId(), command.questionId(),
                        command.selectedOptionIds(), command.openTextAnswer()));

        return examAnswerRepository.save(answer);
    }
}
