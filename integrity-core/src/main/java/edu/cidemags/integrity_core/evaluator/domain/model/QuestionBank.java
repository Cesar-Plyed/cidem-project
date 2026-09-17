package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class QuestionBank {
    private final UUID id;
    private final String name;
    private final String description;
    private final List<Question> questions;

    public QuestionBank(String name, String description) {
        this(UUID.randomUUID(), name, description, new ArrayList<>());
    }

    public QuestionBank(UUID id, String name, String description, List<Question> questions) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del banco no puede estar vacio");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.questions = questions == null ? new ArrayList<>() : new ArrayList<>(questions);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public void addQuestion(Question question) {
        if (question == null) {
            throw new IllegalArgumentException("La pregunta no puede ser nula");
        }
        this.questions.add(question);
    }
}
