package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Exam {
    private final UUID id;
    private String title;
    private String description;
    private final UUID createdByUserId;
    private final OffsetDateTime createdAt;
    private final List<Question> questions;
    private ExamStatus status;

    public Exam(String title, String description, UUID createdByUserId) {
        this(UUID.randomUUID(), title, description, createdByUserId, OffsetDateTime.now(ZoneId.systemDefault()),
                new ArrayList<>(),
                ExamStatus.DRAFT);
    }

    public Exam(UUID id, String title, String description, UUID createdByUserId, OffsetDateTime createdAt,
            List<Question> questions, ExamStatus status) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El titulo del examen no puede estar vacio");
        }
        if (createdByUserId == null) {
            throw new IllegalArgumentException("El creador del examen es obligatorio");
        }
        if (status == null) {
            throw new IllegalArgumentException("El estado del examen es obligatorio");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.questions = questions == null ? new ArrayList<>() : new ArrayList<>(questions);
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void addQuestion(Question question) {
        if (question == null) {
            throw new IllegalArgumentException("La pregunta no puede ser nula");
        }
        this.questions.add(question);
    }

    public void removeQuestion(UUID questionId) {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden eliminar preguntas de un examen en borrador");
        }
        boolean removed = questions.removeIf(question -> question.getId().equals(questionId));
        if (!removed) {
            throw new IllegalArgumentException("La pregunta no pertenece al examen");
        }
    }

    public void updateDetails(String title, String description) {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede editar un examen en borrador");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El titulo del examen no puede estar vacio");
        }
        this.title = title;
        this.description = description;
    }

    public void publish() {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede publicar un examen en borrador");
        }
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("No se puede publicar un examen sin preguntas");
        }
        this.status = ExamStatus.PUBLISHED;
    }

    public void close() {
        if (status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("Solo se puede cerrar un examen publicado");
        }
        this.status = ExamStatus.CLOSED;
    }
}
