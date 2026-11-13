package edu.cidemags.integrity_core.legal.domain.entity;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.DataType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Politica de minimizacion: que dato se captura y durante cuanto tiempo (spec 41). */
@Entity
@Table(name = "data_capture_policy")
public class DataCapturePolicy {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, unique = true, length = 50)
    private DataType dataType;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays = 90;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = true;

    protected DataCapturePolicy() {
    }

    public DataCapturePolicy(DataType dataType, int retentionDays, boolean mandatory) {
        this.dataType = dataType;
        this.retentionDays = retentionDays;
        this.mandatory = mandatory;
    }

    public UUID getId() {
        return id;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
