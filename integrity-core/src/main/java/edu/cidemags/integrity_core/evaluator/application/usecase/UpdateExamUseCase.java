package edu.cidemags.integrity_core.evaluator.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.UpdateExamCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;

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
        exam.updateDetails(command.title(), command.description(),
                command.durationMinutes() == null ? exam.getDurationMinutes() : command.durationMinutes());
        // El editor guarda el listado completo de preguntas; si no viene, no se tocan.
        if (command.questions() != null && !command.questions().isEmpty()) {
            exam.replaceQuestions(toDomainQuestions(command.questions()));
        }
        return examRepository.save(exam);
    }

    private static List<Question> toDomainQuestions(List<CreateQuestionCommand> commands) {
        return commands.stream()
                .map(command -> {
                    List<QuestionOption> options = command.options() == null ? List.of()
                            : command.options().stream()
                                    .map(option -> new QuestionOption(option.text(), option.correct()))
                                    .toList();
                    return new Question(UUID.randomUUID(), command.prompt(), command.type(), options,
                            command.correctAnswerText(), command.points());
                })
                .toList();
    }
}
