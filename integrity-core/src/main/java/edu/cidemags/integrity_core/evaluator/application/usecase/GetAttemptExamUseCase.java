package edu.cidemags.integrity_core.evaluator.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.evaluator.application.dto.AttemptExamResponse;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;

/**
 * Devuelve al alumno el examen de SU intento, sin revelar las respuestas correctas,
 * junto con lo que ya haya respondido (para poder retomar el examen).
 */
@Service
public class GetAttemptExamUseCase {

    private final ExamAttemptRepository attemptRepository;
    private final ExamRepository examRepository;
    private final ExamAnswerRepository answerRepository;

    public GetAttemptExamUseCase(ExamAttemptRepository attemptRepository, ExamRepository examRepository,
            ExamAnswerRepository answerRepository) {
        this.attemptRepository = attemptRepository;
        this.examRepository = examRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional(readOnly = true)
    public AttemptExamResponse execute(UUID attemptId, UUID requesterId) {
        if (attemptId == null || requesterId == null) {
            throw new IllegalArgumentException("El intento y el usuario son obligatorios");
        }

        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ExamNotFoundException(attemptId));
        if (!attempt.getStudentId().equals(requesterId)) {
            throw new AccessDeniedException("No puedes acceder al intento de otro estudiante");
        }

        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(attempt.getExamId()));

        List<AttemptExamResponse.QuestionView> questions = exam.getQuestions().stream()
                .map(question -> new AttemptExamResponse.QuestionView(
                        question.getId(),
                        question.getPrompt(),
                        question.getType(),
                        question.getOptions().stream()
                                .map(option -> new AttemptExamResponse.OptionView(option.getId(), option.getText()))
                                .toList()))
                .toList();

        List<AttemptExamResponse.AnswerView> answers = answerRepository.findByAttemptId(attempt.getId()).stream()
                .map(answer -> new AttemptExamResponse.AnswerView(
                        answer.getQuestionId(),
                        answer.getSelectedOptionIds(),
                        answer.getOpenTextAnswer()))
                .toList();

        return new AttemptExamResponse(attempt.getId(), exam.getId(), exam.getTitle(), exam.getDescription(),
                attempt.getStartedAt(), attempt.isSubmitted(), questions, answers);
    }
}
