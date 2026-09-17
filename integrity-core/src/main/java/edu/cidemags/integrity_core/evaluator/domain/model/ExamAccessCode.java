package edu.cidemags.integrity_core.evaluator.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class ExamAccessCode {
    private final UUID id;
    private final UUID examId;
    private final String code;
    private final OffsetDateTime expiresAt;
    private final boolean active;

    public ExamAccessCode(UUID examId, String code, OffsetDateTime expiresAt) {
        this(UUID.randomUUID(), examId, code, expiresAt, true);
    }

    public ExamAccessCode(UUID id, UUID examId, String code, OffsetDateTime expiresAt, boolean active) {
        if (examId == null) {
            throw new IllegalArgumentException("El examen es obligatorio");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El codigo de acceso no puede estar vacio");
        }
        this.id = id;
        this.examId = examId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now(ZoneId.systemDefault()).isAfter(expiresAt);
    }

    public boolean isValid() {
        return active && !isExpired();
    }

    public UUID getId() {
        return id;
    }

    public UUID getExamId() {
        return examId;
    }

    public String getCode() {
        return code;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }
}
