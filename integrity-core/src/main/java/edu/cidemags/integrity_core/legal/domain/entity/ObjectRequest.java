package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.ObjectStatus;
import edu.cidemags.integrity_core.legal.domain.model.ObjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Derecho de objecion al tratamiento (Art. 21 GDPR, spec 46). */
@Entity
@Table(name = "object_requests")
public class ObjectRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 30)
    private ObjectType objectType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ObjectStatus status = ObjectStatus.PENDING;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note")
    private String resolutionNote;

    protected ObjectRequest() {
    }

    public ObjectRequest(UUID studentId, ObjectType objectType, String reason) {
        this.studentId = studentId;
        this.objectType = objectType;
        this.reason = reason;
    }

    public void resolve(ObjectStatus status, String note) {
        this.status = status;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
    }

    public boolean isActive() {
        return status == ObjectStatus.PENDING || status == ObjectStatus.ACCEPTED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public ObjectStatus getStatus() {
        return status;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }
}
