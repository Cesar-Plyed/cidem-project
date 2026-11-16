package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import edu.cidemags.integrity_core.evaluator.application.dto.AttemptExamResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.ExamResultResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.JoinExamByAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.SubmitExamAttemptCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.SubmitQuestionAnswerCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.StudentExamHistoryResponse;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetAttemptExamUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetExamResultUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.JoinExamByAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.RevokeExamAttemptUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitExamAttemptUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitQuestionAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.infrastructure.web.dto.JoinExamByCodeRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/evaluator")
public class ExamAttemptController {

    /** Header que marca las peticiones del cliente de escritorio Tauri (spec 51). */
    public static final String DESKTOP_CLIENT_HEADER = "X-Cidem-Client";
    private static final String DESKTOP_CLIENT_VALUE = "tauri-desktop";

    private final JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase;
    private final SubmitExamAttemptUseCase submitExamAttemptUseCase;
    private final SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase;
    private final GetExamResultUseCase getExamResultUseCase;
    private final GetAttemptExamUseCase getAttemptExamUseCase;
    private final RevokeExamAttemptUseCase revokeExamAttemptUseCase;

    public ExamAttemptController(JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase,
            SubmitExamAttemptUseCase submitExamAttemptUseCase,
            SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase,
            GetExamResultUseCase getExamResultUseCase,
            GetAttemptExamUseCase getAttemptExamUseCase,
            RevokeExamAttemptUseCase revokeExamAttemptUseCase) {
        this.joinExamByAccessCodeUseCase = joinExamByAccessCodeUseCase;
        this.submitExamAttemptUseCase = submitExamAttemptUseCase;
        this.submitQuestionAnswerUseCase = submitQuestionAnswerUseCase;
        this.getExamResultUseCase = getExamResultUseCase;
        this.getAttemptExamUseCase = getAttemptExamUseCase;
        this.revokeExamAttemptUseCase = revokeExamAttemptUseCase;
    }

    @PostMapping("/attempts/join")
    public ResponseEntity<ExamAttempt> joinExam(@Valid @RequestBody JoinExamByAccessCodeCommand command,
            @RequestHeader(value = DESKTOP_CLIENT_HEADER, required = false) String clientHeader,
            Authentication authentication) {
        UUID studentId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(joinExamByAccessCodeUseCase.execute(
                new JoinExamByAccessCodeCommand(command.examId(), command.code(), studentId),
                isDesktopClient(clientHeader)));
    }

    @PostMapping("/attempts/join-by-code")
    public ResponseEntity<ExamAttempt> joinExamByCode(@Valid @RequestBody JoinExamByCodeRequest request,
            @RequestHeader(value = DESKTOP_CLIENT_HEADER, required = false) String clientHeader,
            Authentication authentication) {
        return ResponseEntity.ok(joinExamByAccessCodeUseCase.executeByCode(
                request.code(), (UUID) authentication.getPrincipal(), isDesktopClient(clientHeader)));
    }

    @GetMapping("/attempts/{attemptId}/exam")
    public ResponseEntity<AttemptExamResponse> getAttemptExam(@PathVariable UUID attemptId,
            Authentication authentication) {
        return ResponseEntity.ok(getAttemptExamUseCase.execute(attemptId, (UUID) authentication.getPrincipal()));
    }

    private boolean isDesktopClient(String clientHeader) {
        return DESKTOP_CLIENT_VALUE.equals(clientHeader);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ExamAttempt> submitExam(@PathVariable UUID attemptId,
            Authentication authentication) {
        UUID studentId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(submitExamAttemptUseCase.execute(new SubmitExamAttemptCommand(attemptId), studentId));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<ExamAnswer> submitAnswer(@PathVariable UUID attemptId,
            @Valid @RequestBody SubmitQuestionAnswerCommand command, Authentication authentication) {
        UUID studentId = (UUID) authentication.getPrincipal();
        SubmitQuestionAnswerCommand fixedCommand = new SubmitQuestionAnswerCommand(
                attemptId,
                command.questionId(),
                command.selectedOptionIds(),
                command.openTextAnswer());
        return ResponseEntity.ok(submitQuestionAnswerUseCase.execute(fixedCommand, studentId));
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ResponseEntity<ExamResultResponse> getResult(@PathVariable UUID attemptId,
            Authentication authentication) {
        boolean administrator = isAdministrator(authentication);
        return ResponseEntity.ok(getExamResultUseCase.execute(
                attemptId, (UUID) authentication.getPrincipal(), administrator));
    }

    @GetMapping("/attempts/history")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<java.util.List<StudentExamHistoryResponse>> getStudentHistory(
            Authentication authentication) {
        return ResponseEntity.ok(getExamResultUseCase.executeStudentHistory(
                (UUID) authentication.getPrincipal()));
    }

    @PostMapping("/attempts/{attemptId}/revoke")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMINISTRATIVO')")
    public ResponseEntity<ExamAttempt> revokeAttempt(@PathVariable UUID attemptId,
            Authentication authentication) {
        boolean administrator = isAdministrator(authentication);
        return ResponseEntity.ok(revokeExamAttemptUseCase.execute(
                attemptId, (UUID) authentication.getPrincipal(), administrator));
    }

    private boolean isAdministrator(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATIVO"));
    }
}
