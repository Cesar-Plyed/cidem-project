package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.UUID;

public class QuestionOption {
    private final UUID id;
    private final String text;
    private final boolean correct;

    public QuestionOption(String text, boolean correct) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("La opcion de pregunta no puede estar vacia");
        }
        this.id = UUID.randomUUID();
        this.text = text;
        this.correct = correct;
    }

    public QuestionOption(UUID id, String text, boolean correct) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("La opcion de pregunta no puede estar vacia");
        }
        this.id = id;
        this.text = text;
        this.correct = correct;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
