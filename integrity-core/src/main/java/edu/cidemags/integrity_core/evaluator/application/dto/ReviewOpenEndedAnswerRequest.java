package edu.cidemags.integrity_core.evaluator.application.dto;

public record ReviewOpenEndedAnswerRequest(int score, String feedback) {
    public ReviewOpenEndedAnswerRequest(int score) {
        this(score, null);
    }
}
