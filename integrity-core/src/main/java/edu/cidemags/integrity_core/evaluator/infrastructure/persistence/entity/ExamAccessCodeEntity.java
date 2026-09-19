package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_access_codes", indexes = {
        @Index(name = "idx_exam_access_code_exam_code", columnList = "exam_id, code", unique = true)
})
public class ExamAccessCodeEntity {

    @Id
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean active;

    protected ExamAccessCodeEntity() {
    }

    public ExamAccessCodeEntity(UUID id, UUID examId, String code, OffsetDateTime expiresAt, boolean active) {
        this.id = id;
        this.examId = examId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
