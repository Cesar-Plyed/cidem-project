package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class ExamAttempt {
    private final UUID id;
    private final UUID examId;
    private final UUID studentId;
    private final OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private boolean submitted;

    public ExamAttempt(UUID examId, UUID studentId) {
        this(UUID.randomUUID(), examId, studentId, OffsetDateTime.now(ZoneId.systemDefault()), null, false);
    }

    public ExamAttempt(UUID id, UUID examId, UUID studentId, OffsetDateTime startedAt,
            OffsetDateTime submittedAt, boolean submitted) {
        if (examId == null) {
            throw new IllegalArgumentException("El examen es obligatorio");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("El estudiante es obligatorio");
        }
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.startedAt = startedAt == null ? OffsetDateTime.now(ZoneId.systemDefault()) : startedAt;
        this.submittedAt = submittedAt;
        this.submitted = submitted;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExamId() {
        return examId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void submit() {
        if (submitted) {
            throw new IllegalStateException("La entrega ya fue registrada");
        }
        this.submittedAt = OffsetDateTime.now(ZoneId.systemDefault());
        this.submitted = true;
    }
}
