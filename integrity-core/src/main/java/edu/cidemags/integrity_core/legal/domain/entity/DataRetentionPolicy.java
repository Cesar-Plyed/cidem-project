package edu.cidemags.integrity_core.legal.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Politica de retencion de datos (spec 38). */
@Entity
@Table(name = "data_retention_policy")
public class DataRetentionPolicy {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "retention_years", nullable = false)
    private int retentionYears = 3;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_types", nullable = false, columnDefinition = "jsonb")
    private List<String> dataTypes = new ArrayList<>();

    @Column(name = "retention_start_date", nullable = false)
    private Instant retentionStartDate = Instant.now();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected DataRetentionPolicy() {
    }

    public DataRetentionPolicy(int retentionYears, List<String> dataTypes) {
        this.retentionYears = retentionYears;
        this.dataTypes = dataTypes == null ? new ArrayList<>() : new ArrayList<>(dataTypes);
    }

    public UUID getId() {
        return id;
    }

    public int getRetentionYears() {
        return retentionYears;
    }

    public void setRetentionYears(int retentionYears) {
        this.retentionYears = retentionYears;
    }

    public List<String> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(List<String> dataTypes) {
        this.dataTypes = dataTypes == null ? new ArrayList<>() : new ArrayList<>(dataTypes);
    }

    public Instant getRetentionStartDate() {
        return retentionStartDate;
    }

    public void setRetentionStartDate(Instant retentionStartDate) {
        this.retentionStartDate = retentionStartDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
