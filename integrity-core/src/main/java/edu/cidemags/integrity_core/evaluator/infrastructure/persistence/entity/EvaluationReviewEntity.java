package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "evaluation_reviews")
public class EvaluationReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "answer_id", nullable = false)
    private UUID answerId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(nullable = false)
    private int score;

    @Column(length = 2000)
    private String feedback;

    protected EvaluationReviewEntity() {
    }

    public EvaluationReviewEntity(UUID id, UUID answerId, UUID reviewerId, int score, String feedback) {
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

    public void setAnswerId(UUID answerId) {
        this.answerId = answerId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
