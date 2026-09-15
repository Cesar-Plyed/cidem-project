package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.SubmitQuestionAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitQuestionAnswerUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAnswerRepository examAnswerRepository;

    public SubmitQuestionAnswerUseCase(ExamAttemptRepository examAttemptRepository,
            ExamAnswerRepository examAnswerRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.examAnswerRepository = examAnswerRepository;
    }

    @Transactional
    public ExamAnswer execute(SubmitQuestionAnswerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }

        ExamAttempt attempt = examAttemptRepository.findById(command.attemptId())
                .orElseThrow(() -> new ExamNotFoundException(command.attemptId()));

        if (attempt.isSubmitted()) {
            throw new IllegalStateException("La entrega ya fue enviada");
        }

        ExamAnswer answer = new ExamAnswer(attempt.getId(), command.questionId());
        if (command.selectedOptionIds() != null) {
            command.selectedOptionIds().forEach(answer::addSelectedOption);
        }
        if (command.openTextAnswer() != null && !command.openTextAnswer().isBlank()) {
            answer = new ExamAnswer(answer.getId(), answer.getAttemptId(), answer.getQuestionId(),
                    answer.getSelectedOptionIds(), command.openTextAnswer());
        }

        return examAnswerRepository.save(answer);
    }
}
