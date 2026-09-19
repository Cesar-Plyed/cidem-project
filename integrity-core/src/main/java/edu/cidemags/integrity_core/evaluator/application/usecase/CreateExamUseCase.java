package edu.cidemags.integrity_core.evaluator.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;

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
        List<CreateQuestionCommand> questions = command.questions() == null ? List.of() : command.questions();
        for (CreateQuestionCommand questionCommand : questions) {
            List<QuestionOption> options = questionCommand.options() == null ? List.of()
                : questionCommand.options().stream()
                    .map(option -> new QuestionOption(option.text(), option.correct()))
                    .toList();
            exam.addQuestion(new Question(UUID.randomUUID(), questionCommand.prompt(), questionCommand.type(),
                options, questionCommand.correctAnswerText()));
        }
        return examRepository.save(exam);
    }
}
