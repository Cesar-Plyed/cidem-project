package edu.cidemags.integrity_core.evaluator.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import edu.cidemags.integrity_core.shared.events.ExamCreatedEvent;

@Service
public class CreateExamUseCase {

    private final ExamRepository examRepository;
    private final ApplicationEventPublisher publisher;

    public CreateExamUseCase(ExamRepository examRepository, ApplicationEventPublisher publisher) {
        this.examRepository = examRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Exam execute(CreateExamCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        Exam exam = new Exam(command.title(), command.description(), command.createdByUserId(),
                command.durationMinutes() == null ? 30 : command.durationMinutes());
        exam.setBrowserAllowed(command.browserAllowedOrDefault());
        List<CreateQuestionCommand> questions = command.questions() == null ? List.of() : command.questions();
        for (CreateQuestionCommand questionCommand : questions) {
            List<QuestionOption> options = questionCommand.options() == null ? List.of()
                : questionCommand.options().stream()
                    .map(option -> new QuestionOption(option.text(), option.correct()))
                    .toList();
            // addQuestion valida además que la suma de puntos no exceda 10.
            exam.addQuestion(new Question(UUID.randomUUID(), questionCommand.prompt(), questionCommand.type(),
                options, questionCommand.correctAnswerText(), questionCommand.points()));
        }
        Exam saved = examRepository.save(exam);
        // SAFEGUARD escucha este evento para crear su configuración por defecto.
        publisher.publishEvent(new ExamCreatedEvent(saved.getId(), saved.getTitle(), saved.getCreatedByUserId(),
                saved.getCreatedAt()));
        return saved;
    }
}
