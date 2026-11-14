package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.ErasureStatus;
import edu.cidemags.integrity_core.legal.domain.model.ErasureType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Solicitud de borrado (Art. 17 GDPR, spec 44). */
@Entity
@Table(name = "erasure_requests")
public class ErasureRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "erasure_type", nullable = false, length = 10)
    private ErasureType erasureType;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ErasureStatus status = ErasureStatus.PENDING;

    protected ErasureRequest() {
    }

    public ErasureRequest(UUID studentId, ErasureType erasureType) {
        this.studentId = studentId;
        this.erasureType = erasureType;
    }

    public void complete() {
        this.status = ErasureStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail() {
        this.status = ErasureStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public ErasureType getErasureType() {
        return erasureType;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public ErasureStatus getStatus() {
        return status;
    }
}
