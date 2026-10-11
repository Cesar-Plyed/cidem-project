package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Exam {
    /** La nota del examen se expresa sobre 10: la suma de puntos de sus preguntas no puede exceder este tope. */
    public static final int MAX_TOTAL_POINTS = 10;

    private final UUID id;
    private String title;
    private String description;
    private final UUID createdByUserId;
    private final OffsetDateTime createdAt;
    private int durationMinutes;
    private final List<Question> questions;
    private ExamStatus status;

    public Exam(String title, String description, UUID createdByUserId) {
        this(title, description, createdByUserId, 30);
    }

    public Exam(String title, String description, UUID createdByUserId, int durationMinutes) {
        this(UUID.randomUUID(), title, description, createdByUserId, OffsetDateTime.now(ZoneId.systemDefault()),
                durationMinutes, new ArrayList<>(), ExamStatus.DRAFT);
    }

    public Exam(UUID id, String title, String description, UUID createdByUserId, OffsetDateTime createdAt,
            List<Question> questions, ExamStatus status) {
        this(id, title, description, createdByUserId, createdAt, 30, questions, status);
    }

    public Exam(UUID id, String title, String description, UUID createdByUserId, OffsetDateTime createdAt,
            int durationMinutes, List<Question> questions, ExamStatus status) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El titulo del examen no puede estar vacio");
        }
        if (createdByUserId == null) {
            throw new IllegalArgumentException("El creador del examen es obligatorio");
        }
        if (status == null) {
            throw new IllegalArgumentException("El estado del examen es obligatorio");
        }
        if (durationMinutes < 1) {
            throw new IllegalArgumentException("La duracion del examen debe ser al menos un minuto");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.durationMinutes = durationMinutes;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    /** Suma de puntos de todas las preguntas: es el puntaje maximo alcanzable en el examen. */
    public int getTotalPoints() {
        return questions.stream().mapToInt(Question::getPoints).sum();
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void addQuestion(Question question) {
        if (question == null) {
            throw new IllegalArgumentException("La pregunta no puede ser nula");
        }
        if (getTotalPoints() + question.getPoints() > MAX_TOTAL_POINTS) {
            throw new IllegalArgumentException(
                    "La suma de puntos del examen no puede exceder " + MAX_TOTAL_POINTS);
        }
        this.questions.add(question);
    }

    /**
     * Reemplaza todas las preguntas del examen (solo en borrador): el editor guarda
     * el listado completo. En un examen publicado/cerrado no se tocan las preguntas
     * porque sus respuestas ya apuntan a ellas.
     */
    public void replaceQuestions(List<Question> newQuestions) {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden editar las preguntas de un examen en borrador");
        }
        if (newQuestions == null || newQuestions.isEmpty()) {
            throw new IllegalArgumentException("El examen debe tener al menos una pregunta");
        }
        int total = newQuestions.stream().mapToInt(Question::getPoints).sum();
        if (total > MAX_TOTAL_POINTS) {
            throw new IllegalArgumentException("La suma de puntos del examen no puede exceder " + MAX_TOTAL_POINTS);
        }
        this.questions.clear();
        this.questions.addAll(newQuestions);
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
        updateDetails(title, description, durationMinutes);
    }

    public void updateDetails(String title, String description, int durationMinutes) {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede editar un examen en borrador");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El titulo del examen no puede estar vacio");
        }
        if (durationMinutes < 1) {
            throw new IllegalArgumentException("La duracion del examen debe ser al menos un minuto");
        }
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
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
