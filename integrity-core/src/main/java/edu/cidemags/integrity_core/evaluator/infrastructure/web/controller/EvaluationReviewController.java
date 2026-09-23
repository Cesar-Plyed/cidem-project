package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import edu.cidemags.integrity_core.evaluator.application.dto.OpenEndedReviewResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerRequest;
import edu.cidemags.integrity_core.evaluator.application.usecase.ListOpenEndedReviewsUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ReviewOpenEndedAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluator")
public class EvaluationReviewController {

    private final ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase;
    private final ListOpenEndedReviewsUseCase listOpenEndedReviewsUseCase;

    public EvaluationReviewController(ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase,
            ListOpenEndedReviewsUseCase listOpenEndedReviewsUseCase) {
        this.reviewOpenEndedAnswerUseCase = reviewOpenEndedAnswerUseCase;
        this.listOpenEndedReviewsUseCase = listOpenEndedReviewsUseCase;
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATIVO"));
    }

    @PostMapping("/reviews/open-ended")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<EvaluationReview> reviewOpenEndedAnswer(@RequestBody ReviewOpenEndedAnswerCommand command,
            Authentication authentication) {
        UUID reviewerId = (UUID) authentication.getPrincipal();
        boolean administrator = isAdministrator(authentication);
        return ResponseEntity.ok(reviewOpenEndedAnswerUseCase.execute(new ReviewOpenEndedAnswerCommand(
                command.answerId(), reviewerId, command.score(), command.feedback(), administrator)));
    }

    @PutMapping("/reviews/open-ended/{answerId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<EvaluationReview> reviseOpenEndedAnswer(@PathVariable UUID answerId,
            @RequestBody ReviewOpenEndedAnswerRequest request, Authentication authentication) {
        UUID reviewerId = (UUID) authentication.getPrincipal();
        boolean administrator = isAdministrator(authentication);
        return ResponseEntity.ok(reviewOpenEndedAnswerUseCase.execute(new ReviewOpenEndedAnswerCommand(
                answerId, reviewerId, request.score(), request.feedback(), administrator)));
    }

    @GetMapping("/exams/{examId}/reviews/open-ended")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<List<OpenEndedReviewResponse>> listOpenEndedReviews(
            @PathVariable UUID examId,
            @RequestParam(defaultValue = "true") boolean pending,
            Authentication authentication) {
        UUID requesterId = (UUID) authentication.getPrincipal();
        boolean administrator = isAdministrator(authentication);
        return ResponseEntity.ok(listOpenEndedReviewsUseCase.execute(examId, requesterId, administrator, pending));
    }
}
