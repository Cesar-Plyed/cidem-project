package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import edu.cidemags.integrity_core.evaluator.application.dto.CreateExamCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.GenerateExamAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.CreateExamUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GenerateExamAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/evaluator")
public class ExamController {

    private final CreateExamUseCase createExamUseCase;
    private final GenerateExamAccessCodeUseCase generateExamAccessCodeUseCase;

    public ExamController(CreateExamUseCase createExamUseCase,
            GenerateExamAccessCodeUseCase generateExamAccessCodeUseCase) {
        this.createExamUseCase = createExamUseCase;
        this.generateExamAccessCodeUseCase = generateExamAccessCodeUseCase;
    }

    @PostMapping("/exams")
    public ResponseEntity<Exam> createExam(@RequestBody CreateExamCommand command) {
        return ResponseEntity.ok(createExamUseCase.execute(command));
    }

    @PostMapping("/exams/{examId}/access-code")
    public ResponseEntity<ExamAccessCode> generateAccessCode(@PathVariable UUID examId,
            @RequestParam(defaultValue = "20") int expiresInMinutes,
            @RequestParam(required = false) String code) {
        GenerateExamAccessCodeCommand command = new GenerateExamAccessCodeCommand(examId, code, expiresInMinutes);
        return ResponseEntity.ok(generateExamAccessCodeUseCase.execute(command));
    }
}
