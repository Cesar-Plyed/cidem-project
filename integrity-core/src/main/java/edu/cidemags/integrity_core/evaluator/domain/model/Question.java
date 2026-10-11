package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Question {
    /** Valor por defecto de una pregunta creada sin puntaje explicito (p. ej. importada de un banco). */
    public static final int DEFAULT_POINTS = 1;
    /** Puntaje maximo por pregunta: la suma del examen no puede exceder 10 (Exam.MAX_TOTAL_POINTS). */
    public static final int MAX_POINTS = 10;

    private final UUID id;
    private final String prompt;
    private final QuestionType type;
    private final List<QuestionOption> options;
    private final String correctAnswerText;
    private final int points;

    public Question(String prompt, QuestionType type) {
        this(UUID.randomUUID(), prompt, type, new ArrayList<>(), null, DEFAULT_POINTS);
    }

    public Question(UUID id, String prompt, QuestionType type, List<QuestionOption> options, String correctAnswerText) {
        this(id, prompt, type, options, correctAnswerText, DEFAULT_POINTS);
    }

    public Question(UUID id, String prompt, QuestionType type, List<QuestionOption> options, String correctAnswerText,
            int points) {
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
        if (points < 1 || points > MAX_POINTS) {
            throw new IllegalArgumentException("Los puntos de la pregunta deben estar entre 1 y " + MAX_POINTS);
        }
        this.id = id;
        this.prompt = prompt;
        this.type = type;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
        this.correctAnswerText = correctAnswerText;
        this.points = points;
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

    public int getPoints() {
        return points;
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
