package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamAttempt {
    private final UUID id;
    private final UUID examId;
    private final UUID studentId;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime submittedAt;
    private final boolean submitted;

    public ExamAttempt(UUID examId, UUID studentId) {
        this(UUID.randomUUID(), examId, studentId, OffsetDateTime.now(), null, false);
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
        this.startedAt = startedAt;
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
        this.submittedAt = OffsetDateTime.now();
    }
}
