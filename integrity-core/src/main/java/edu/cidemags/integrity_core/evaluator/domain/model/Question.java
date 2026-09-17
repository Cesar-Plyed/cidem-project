package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Question {
    private final UUID id;
    private final String prompt;
    private final QuestionType type;
    private final List<QuestionOption> options;
    private final String correctAnswerText;

    public Question(String prompt, QuestionType type) {
        this(UUID.randomUUID(), prompt, type, new ArrayList<>(), null);
    }

    public Question(UUID id, String prompt, QuestionType type, List<QuestionOption> options, String correctAnswerText) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacia");
        }
        if (type == null) {
            throw new IllegalArgumentException("El tipo de pregunta es obligatorio");
        }
        if (type != QuestionType.OPEN_ENDED && (options == null || options.isEmpty())) {
            throw new IllegalArgumentException("Las preguntas cerradas requieren opciones");
        }
        if (type == QuestionType.OPEN_ENDED && options != null && !options.isEmpty()) {
            throw new IllegalArgumentException("Las preguntas abiertas no usan opciones");
        }
        this.id = id;
        this.prompt = prompt;
        this.type = type;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
        this.correctAnswerText = correctAnswerText;
    }

    public UUID getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public QuestionType getType() {
        return type;
    }

    public List<QuestionOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public String getCorrectAnswerText() {
        return correctAnswerText;
    }

    public void addOption(QuestionOption option) {
        if (type == QuestionType.OPEN_ENDED) {
            throw new IllegalArgumentException("Una pregunta abierta no admite opciones");
        }
        if (option == null) {
            throw new IllegalArgumentException("La opcion no puede ser nula");
        }
        this.options.add(option);
    }
}
