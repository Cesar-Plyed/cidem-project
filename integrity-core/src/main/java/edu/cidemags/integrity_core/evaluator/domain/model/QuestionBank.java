package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class QuestionBank {
    private final UUID id;
    private final UUID createdByUserId;
    private String name;
    private String description;
    private final List<Question> questions;

    public QuestionBank(String name, String description) {
        this(UUID.randomUUID(), name, description, null, new ArrayList<>());
    }

    public QuestionBank(UUID id, String name, String description, List<Question> questions) {
        this(id, name, description, null, questions);
    }

    public QuestionBank(UUID id, String name, String description, UUID createdByUserId, List<Question> questions) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del banco no puede estar vacio");
        }
        this.id = id;
        this.createdByUserId = createdByUserId;
        this.name = name;
        this.description = description;
        this.questions = questions == null ? new ArrayList<>() : new ArrayList<>(questions);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
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

    public void updateDetails(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del banco no puede estar vacio");
        }
        this.name = name;
        this.description = description;
    }
}
