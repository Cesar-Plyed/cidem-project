package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionBankCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.CreateQuestionCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.UpdateQuestionBankCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.CreateQuestionBankUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.QuestionBankManagementUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionBank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/evaluator/question-banks")
public class QuestionBankController {
    private final CreateQuestionBankUseCase createQuestionBankUseCase;
    private final QuestionBankManagementUseCase managementUseCase;

    public QuestionBankController(CreateQuestionBankUseCase createQuestionBankUseCase,
            QuestionBankManagementUseCase managementUseCase) {
        this.createQuestionBankUseCase = createQuestionBankUseCase;
        this.managementUseCase = managementUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<QuestionBank> create(@Valid @RequestBody CreateQuestionBankCommand command,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createQuestionBankUseCase.execute(command,
                (UUID) authentication.getPrincipal()));
    }

    @GetMapping
    public ResponseEntity<List<QuestionBank>> list(Authentication authentication) {
        return ResponseEntity.ok(managementUseCase.listFor((UUID) authentication.getPrincipal(),
                isAdministrator(authentication)));
    }

    @GetMapping("/{bankId}")
    public ResponseEntity<QuestionBank> get(@PathVariable UUID bankId, Authentication authentication) {
        return ResponseEntity.ok(managementUseCase.getFor(bankId, (UUID) authentication.getPrincipal(),
                isAdministrator(authentication)));
    }

    @PostMapping("/{bankId}/questions")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<QuestionBank> addQuestion(@PathVariable UUID bankId,
            @Valid @RequestBody CreateQuestionCommand command, Authentication authentication) {
        managementUseCase.assertCanManage(bankId, (UUID) authentication.getPrincipal(),
                isAdministrator(authentication));
        return ResponseEntity.ok(managementUseCase.addQuestion(bankId, command));
    }

    @PutMapping("/{bankId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<QuestionBank> update(@PathVariable UUID bankId,
            @Valid @RequestBody UpdateQuestionBankCommand command, Authentication authentication) {
        managementUseCase.assertCanManage(bankId, (UUID) authentication.getPrincipal(),
                isAdministrator(authentication));
        return ResponseEntity.ok(managementUseCase.update(
                new UpdateQuestionBankCommand(bankId, command.name(), command.description())));
    }

    @DeleteMapping("/{bankId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID bankId, Authentication authentication) {
        managementUseCase.assertCanManage(bankId, (UUID) authentication.getPrincipal(),
                isAdministrator(authentication));
        managementUseCase.delete(bankId);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATIVO"));
    }
}