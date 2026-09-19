package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cidemags.integrity_core.evaluator.application.dto.AttemptExamResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.ExamResultResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.JoinExamByAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.SubmitExamAttemptCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.SubmitQuestionAnswerCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetAttemptExamUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetExamResultUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.JoinExamByAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitExamAttemptUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitQuestionAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.infrastructure.web.dto.JoinExamByCodeRequest;

@RestController
@RequestMapping("/api/evaluator")
public class ExamAttemptController {

    private final JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase;
    private final SubmitExamAttemptUseCase submitExamAttemptUseCase;
    private final SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase;
    private final GetExamResultUseCase getExamResultUseCase;
    private final GetAttemptExamUseCase getAttemptExamUseCase;

    public ExamAttemptController(JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase,
            SubmitExamAttemptUseCase submitExamAttemptUseCase,
            SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase,
            GetExamResultUseCase getExamResultUseCase,
            GetAttemptExamUseCase getAttemptExamUseCase) {
        this.joinExamByAccessCodeUseCase = joinExamByAccessCodeUseCase;
        this.submitExamAttemptUseCase = submitExamAttemptUseCase;
        this.submitQuestionAnswerUseCase = submitQuestionAnswerUseCase;
        this.getExamResultUseCase = getExamResultUseCase;
        this.getAttemptExamUseCase = getAttemptExamUseCase;
    }

    @PostMapping("/attempts/join")
    public ResponseEntity<ExamAttempt> joinExam(@RequestBody JoinExamByAccessCodeCommand command,
            Authentication authentication) {
        UUID studentId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(joinExamByAccessCodeUseCase.execute(
                new JoinExamByAccessCodeCommand(command.examId(), command.code(), studentId)));
    }

    @PostMapping("/attempts/join-by-code")
    public ResponseEntity<ExamAttempt> joinExamByCode(@RequestBody JoinExamByCodeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(joinExamByAccessCodeUseCase.executeByCode(
                request.code(), (UUID) authentication.getPrincipal()));
    }

    @GetMapping("/attempts/{attemptId}/exam")
    public ResponseEntity<AttemptExamResponse> getAttemptExam(@PathVariable UUID attemptId,
            Authentication authentication) {
        return ResponseEntity.ok(getAttemptExamUseCase.execute(attemptId, (UUID) authentication.getPrincipal()));
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

    @GetMapping("/attempts/{attemptId}/result")
    public ResponseEntity<ExamResultResponse> getResult(@PathVariable UUID attemptId,
            Authentication authentication) {
        return ResponseEntity.ok(getExamResultUseCase.execute(attemptId, (UUID) authentication.getPrincipal()));
    }
}
