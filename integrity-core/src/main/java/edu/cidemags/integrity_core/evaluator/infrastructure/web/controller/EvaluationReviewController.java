package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.application.usecase.ReviewOpenEndedAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluator")
public class EvaluationReviewController {

    private final ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase;

    public EvaluationReviewController(ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase) {
        this.reviewOpenEndedAnswerUseCase = reviewOpenEndedAnswerUseCase;
    }

    @PostMapping("/reviews/open-ended")
    public ResponseEntity<EvaluationReview> reviewOpenEndedAnswer(@RequestBody ReviewOpenEndedAnswerCommand command) {
        return ResponseEntity.ok(reviewOpenEndedAnswerUseCase.execute(command));
    }
}
