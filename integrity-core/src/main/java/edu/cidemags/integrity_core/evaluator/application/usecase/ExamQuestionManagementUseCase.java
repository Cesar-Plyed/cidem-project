package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import edu.cidemags.integrity_core.evaluator.domain.port.QuestionBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExamQuestionManagementUseCase {
    private final ExamRepository examRepository;
    private final QuestionBankRepository questionBankRepository;

    public ExamQuestionManagementUseCase(ExamRepository examRepository,
            QuestionBankRepository questionBankRepository) {
        this.examRepository = examRepository;
        this.questionBankRepository = questionBankRepository;
    }

    @Transactional
    public Exam add(UUID examId, CreateQuestionCommand command) {
        if (examId == null || command == null) {
            throw new IllegalArgumentException("El examen y la pregunta son obligatorios");
        }
        Exam exam = find(examId);
        if (exam.getStatus() != edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden agregar preguntas a un examen en borrador");
        }
        List<QuestionOption> options = command.options() == null ? List.of()
                : command.options().stream().map(option -> new QuestionOption(option.text(), option.correct()))
                        .toList();
        // addQuestion valida además que la suma de puntos no exceda el tope del examen.
        exam.addQuestion(new Question(UUID.randomUUID(), command.prompt(), command.type(), options,
                command.correctAnswerText(), command.points()));
        return examRepository.save(exam);
    }

    @Transactional
    public Exam remove(UUID examId, UUID questionId) {
        Exam exam = find(examId);
        exam.removeQuestion(questionId);
        return examRepository.save(exam);
    }

    @Transactional
    public Exam importFromBank(UUID examId, UUID bankId) {
        Exam exam = find(examId);
        QuestionBank bank = questionBankRepository.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("El banco de preguntas no existe"));
        if (exam.getStatus() != edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden importar preguntas a un examen en borrador");
        }
        bank.getQuestions().forEach(question -> exam.addQuestion(new Question(UUID.randomUUID(),
                question.getPrompt(), question.getType(), question.getOptions().stream()
                        .map(option -> new QuestionOption(UUID.randomUUID(), option.getText(), option.isCorrect()))
                        .toList(),
                question.getCorrectAnswerText(), question.getPoints())));
        return examRepository.save(exam);
    }

    private Exam find(UUID examId) {
        return examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException(examId));
    }
}