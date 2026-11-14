package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.DPIAStatus;
import edu.cidemags.integrity_core.legal.domain.model.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Evaluacion de impacto en proteccion de datos (Art. 35 GDPR, spec 50). */
@Entity
@Table(name = "dpia")
public class Dpia {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mitigation_measures", nullable = false, columnDefinition = "jsonb")
    private List<String> mitigationMeasures = new ArrayList<>();

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt = Instant.now();

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private DPIAStatus status = DPIAStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Dpia() {
    }

    public Dpia(String title, String description, RiskLevel riskLevel, List<String> mitigationMeasures) {
        this.title = title;
        this.description = description;
        this.riskLevel = riskLevel;
        this.mitigationMeasures = mitigationMeasures == null ? new ArrayList<>() : new ArrayList<>(mitigationMeasures);
    }

    public void review(UUID reviewedBy, DPIAStatus status) {
        this.reviewedBy = reviewedBy;
        this.status = status;
        this.reviewedAt = Instant.now();
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

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getMitigationMeasures() {
        return mitigationMeasures;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public DPIAStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
