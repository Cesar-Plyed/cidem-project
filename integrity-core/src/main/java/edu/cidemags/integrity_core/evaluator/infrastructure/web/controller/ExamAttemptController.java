package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import edu.cidemags.integrity_core.evaluator.application.dto.JoinExamByAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.SubmitExamAttemptCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.SubmitQuestionAnswerCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.JoinExamByAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitExamAttemptUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitQuestionAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/evaluator")
public class ExamAttemptController {

    private final JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase;
    private final SubmitExamAttemptUseCase submitExamAttemptUseCase;
    private final SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase;

    public ExamAttemptController(JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase,
            SubmitExamAttemptUseCase submitExamAttemptUseCase,
            SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase) {
        this.joinExamByAccessCodeUseCase = joinExamByAccessCodeUseCase;
        this.submitExamAttemptUseCase = submitExamAttemptUseCase;
        this.submitQuestionAnswerUseCase = submitQuestionAnswerUseCase;
    }

    @PostMapping("/attempts/join")
    public ResponseEntity<ExamAttempt> joinExam(@RequestBody JoinExamByAccessCodeCommand command) {
        return ResponseEntity.ok(joinExamByAccessCodeUseCase.execute(command));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ExamAttempt> submitExam(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(submitExamAttemptUseCase.execute(new SubmitExamAttemptCommand(attemptId)));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<ExamAnswer> submitAnswer(@PathVariable UUID attemptId,
            @RequestBody SubmitQuestionAnswerCommand command) {
        SubmitQuestionAnswerCommand fixedCommand = new SubmitQuestionAnswerCommand(
                attemptId,
                command.questionId(),
                command.selectedOptionIds(),
                command.openTextAnswer());
        return ResponseEntity.ok(submitQuestionAnswerUseCase.execute(fixedCommand));
    }
}
