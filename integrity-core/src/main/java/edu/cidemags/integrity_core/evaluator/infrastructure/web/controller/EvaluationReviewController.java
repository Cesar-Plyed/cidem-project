package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.ReviewOpenEndedAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluator")
public class EvaluationReviewController {

    private final ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase;

    public EvaluationReviewController(ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase) {
        this.reviewOpenEndedAnswerUseCase = reviewOpenEndedAnswerUseCase;
    }

    @PostMapping("/reviews/open-ended")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<EvaluationReview> reviewOpenEndedAnswer(@RequestBody ReviewOpenEndedAnswerCommand command,
            Authentication authentication) {
        return ResponseEntity.ok(reviewOpenEndedAnswerUseCase.execute(new ReviewOpenEndedAnswerCommand(
                command.answerId(), (java.util.UUID) authentication.getPrincipal(), command.score(),
                command.feedback())));
    }
}
