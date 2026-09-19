package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.ExamResultResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.GenerateExamAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.PagedExamResultsResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.UpdateExamCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.CreateExamUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ExamManagementUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ExamQuestionManagementUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ExportExamResultsUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GenerateExamAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetExamResultUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;

@RestController
@RequestMapping("/api/evaluator")
public class ExamController {

    private final CreateExamUseCase createExamUseCase;
    private final GenerateExamAccessCodeUseCase generateExamAccessCodeUseCase;
    private final ExamManagementUseCase examManagementUseCase;
    private final ExamQuestionManagementUseCase questionManagementUseCase;
    private final GetExamResultUseCase getExamResultUseCase;
    private final ExportExamResultsUseCase exportExamResultsUseCase;

    public ExamController(CreateExamUseCase createExamUseCase,
            GenerateExamAccessCodeUseCase generateExamAccessCodeUseCase,
            ExamManagementUseCase examManagementUseCase,
            ExamQuestionManagementUseCase questionManagementUseCase,
            GetExamResultUseCase getExamResultUseCase,
            ExportExamResultsUseCase exportExamResultsUseCase) {
        this.createExamUseCase = createExamUseCase;
        this.generateExamAccessCodeUseCase = generateExamAccessCodeUseCase;
        this.examManagementUseCase = examManagementUseCase;
        this.questionManagementUseCase = questionManagementUseCase;
        this.getExamResultUseCase = getExamResultUseCase;
        this.exportExamResultsUseCase = exportExamResultsUseCase;
    }

    @PostMapping("/exams")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> createExam(@RequestBody CreateExamCommand command, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(createExamUseCase.execute(
            new CreateExamCommand(command.title(), command.description(), userId, command.questions())));
    }

    @GetMapping("/exams")
    public ResponseEntity<List<Exam>> listExams() {
        return ResponseEntity.ok(examManagementUseCase.list());
    }

    @GetMapping("/exams/{examId}")
    public ResponseEntity<Exam> getExam(@PathVariable UUID examId) {
        return ResponseEntity.ok(examManagementUseCase.get(examId));
    }

    @PutMapping("/exams/{examId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> updateExam(@PathVariable UUID examId, @RequestBody UpdateExamCommand command,
            Authentication authentication) {
        assertCanManage(examId, authentication);
        return ResponseEntity.ok(examManagementUseCase.update(
                new UpdateExamCommand(examId, command.title(), command.description())));
    }

    @DeleteMapping("/exams/{examId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Void> deleteExam(@PathVariable UUID examId, Authentication authentication) {
        assertCanManage(examId, authentication);
        examManagementUseCase.delete(examId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exams/{examId}/publish")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> publishExam(@PathVariable UUID examId, Authentication authentication) {
        assertCanManage(examId, authentication);
        return ResponseEntity.ok(examManagementUseCase.publish(examId));
    }

    @PostMapping("/exams/{examId}/close")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> closeExam(@PathVariable UUID examId, Authentication authentication) {
        assertCanManage(examId, authentication);
        return ResponseEntity.ok(examManagementUseCase.close(examId));
    }

    @PostMapping("/exams/{examId}/questions")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> addQuestion(@PathVariable UUID examId,
            @RequestBody CreateQuestionCommand command, Authentication authentication) {
        assertCanManage(examId, authentication);
        return ResponseEntity.ok(questionManagementUseCase.add(examId, command));
    }

    @DeleteMapping("/exams/{examId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> removeQuestion(@PathVariable UUID examId, @PathVariable UUID questionId,
            Authentication authentication) {
        assertCanManage(examId, authentication);
        return ResponseEntity.ok(questionManagementUseCase.remove(examId, questionId));
    }

    @PostMapping("/exams/{examId}/question-banks/{bankId}/import")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Exam> importQuestions(@PathVariable UUID examId, @PathVariable UUID bankId,
            Authentication authentication) {
        assertCanManage(examId, authentication);
        return ResponseEntity.ok(questionManagementUseCase.importFromBank(examId, bankId));
    }

    @PostMapping("/exams/{examId}/access-code")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<ExamAccessCode> generateAccessCode(@PathVariable UUID examId,
            @RequestParam(defaultValue = "20") int expiresInMinutes,
            @RequestParam(required = false) String code, Authentication authentication) {
        assertCanManage(examId, authentication);
        GenerateExamAccessCodeCommand command = new GenerateExamAccessCodeCommand(examId, code, expiresInMinutes);
        return ResponseEntity.ok(generateExamAccessCodeUseCase.execute(command));
    }

    @GetMapping("/exams/{examId}/results")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<List<ExamResultResponse>> getExamResults(@PathVariable UUID examId,
            Authentication authentication) {
        UUID requesterId = (UUID) authentication.getPrincipal();
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATIVO"));
        return ResponseEntity.ok(getExamResultUseCase.executeForExam(examId, requesterId, administrator));
    }

    @GetMapping("/exams/{examId}/results/page")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<PagedExamResultsResponse> getExamResultsPage(@PathVariable UUID examId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID requesterId = (UUID) authentication.getPrincipal();
        boolean administrator = isAdministrator(authentication);
        return ResponseEntity
                .ok(getExamResultUseCase.executeForExamPage(examId, requesterId, administrator, page, size));
    }

    @GetMapping(value = "/exams/{examId}/results/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<byte[]> exportExamResults(@PathVariable UUID examId, Authentication authentication) {
        UUID requesterId = (UUID) authentication.getPrincipal();
        byte[] content = exportExamResultsUseCase.execute(examId, requesterId, isAdministrator(authentication))
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=exam-" + examId + "-results.csv")
                .body(content);
    }

    private void assertCanManage(UUID examId, Authentication authentication) {
        if (isAdministrator(authentication)) {
            return;
        }
        UUID requesterId = (UUID) authentication.getPrincipal();
        if (!examManagementUseCase.get(examId).getCreatedByUserId().equals(requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permisos sobre este examen");
        }
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATIVO"));
    }
}
