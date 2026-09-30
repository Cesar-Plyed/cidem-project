package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class EvaluationReview {
    private final UUID id;
    private final UUID answerId;
    private final UUID reviewerId;
    private final int score;
    private final String feedback;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public EvaluationReview(UUID answerId, UUID reviewerId, int score, String feedback) {
        this(UUID.randomUUID(), answerId, reviewerId, score, feedback, OffsetDateTime.now(ZoneId.systemDefault()),
                OffsetDateTime.now(ZoneId.systemDefault()));
    }

    public EvaluationReview(UUID id, UUID answerId, UUID reviewerId, int score, String feedback,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        if (answerId == null) {
            throw new IllegalArgumentException("La respuesta es obligatoria");
        }
        if (reviewerId == null) {
            throw new IllegalArgumentException("El revisor es obligatorio");
        }
        if (score < 0) {
            throw new IllegalArgumentException("La puntuacion no puede ser negativa");
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneId.systemDefault());
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now(ZoneId.systemDefault());
        }
        this.id = id;
        this.answerId = answerId;
        this.reviewerId = reviewerId;
        this.score = score;
        this.feedback = feedback;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public EvaluationReview withScoreAndFeedback(int score, String feedback, UUID reviewerId) {
        return new EvaluationReview(this.id, this.answerId, reviewerId, score, feedback, this.createdAt,
                OffsetDateTime.now(ZoneId.systemDefault()));
    }
}
