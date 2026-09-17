package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ReviewOpenEndedAnswerCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewOpenEndedAnswerUseCaseTest {

    @Test
    void shouldPersistManualReviewForOpenEndedAnswer() {
        EvaluationReviewRepository repository = mock(EvaluationReviewRepository.class);
        ReviewOpenEndedAnswerUseCase useCase = new ReviewOpenEndedAnswerUseCase(repository);

        UUID answerId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        when(repository.save(any(EvaluationReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationReview review = useCase.execute(
                new ReviewOpenEndedAnswerCommand(answerId, reviewerId, 8, "Buena respuesta, bien argumentada."));

        assertNotNull(review);
        assertEquals(answerId, review.getAnswerId());
        assertEquals(8, review.getScore());
        assertEquals("Buena respuesta, bien argumentada.", review.getFeedback());
        verify(repository).save(any(EvaluationReview.class));
    }
}
