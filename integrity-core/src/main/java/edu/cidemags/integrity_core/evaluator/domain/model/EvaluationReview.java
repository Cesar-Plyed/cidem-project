package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.UUID;

public class EvaluationReview {
    private final UUID id;
    private final UUID answerId;
    private final UUID reviewerId;
    private final int score;
    private final String feedback;

    public EvaluationReview(UUID answerId, UUID reviewerId, int score, String feedback) {
        this(UUID.randomUUID(), answerId, reviewerId, score, feedback);
    }

    public EvaluationReview(UUID id, UUID answerId, UUID reviewerId, int score, String feedback) {
        if (answerId == null) {
            throw new IllegalArgumentException("La respuesta es obligatoria");
        }
        if (reviewerId == null) {
            throw new IllegalArgumentException("El revisor es obligatorio");
        }
        if (score < 0) {
            throw new IllegalArgumentException("La puntuacion no puede ser negativa");
        }
        this.id = id;
        this.answerId = answerId;
        this.reviewerId = reviewerId;
        this.score = score;
        this.feedback = feedback;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAnswerId() {
        return answerId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public int getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}
