package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safeguard_configuration")
public class SafeguardConfigurationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "exam_id", nullable = false, unique = true)
    private UUID examId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "require_camera", nullable = false)
    private boolean requireCamera = false;

    @Column(name = "require_microphone", nullable = false)
    private boolean requireMicrophone = false;

    @Column(name = "require_fullscreen", nullable = false)
    private boolean requireFullscreen = false;

    @Column(name = "block_copy_paste", nullable = false)
    private boolean blockCopyPaste = false;

    @Column(name = "max_tab_switches", nullable = false)
    private int maxTabSwitches = 3;

    @Column(name = "risk_threshold", nullable = false)
    private int riskThreshold = 100;

    @Column(name = "auto_terminate_on_threshold", nullable = false)
    private boolean autoTerminateOnThreshold = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequireCamera() {
        return requireCamera;
    }

    public void setRequireCamera(boolean requireCamera) {
        this.requireCamera = requireCamera;
    }

    public boolean isRequireMicrophone() {
        return requireMicrophone;
    }

    public void setRequireMicrophone(boolean requireMicrophone) {
        this.requireMicrophone = requireMicrophone;
    }

    public boolean isRequireFullscreen() {
        return requireFullscreen;
    }

    public void setRequireFullscreen(boolean requireFullscreen) {
        this.requireFullscreen = requireFullscreen;
    }

    public boolean isBlockCopyPaste() {
        return blockCopyPaste;
    }

    public void setBlockCopyPaste(boolean blockCopyPaste) {
        this.blockCopyPaste = blockCopyPaste;
    }

    public int getMaxTabSwitches() {
        return maxTabSwitches;
    }

    public void setMaxTabSwitches(int maxTabSwitches) {
        this.maxTabSwitches = maxTabSwitches;
    }

    public int getRiskThreshold() {
        return riskThreshold;
    }

    public void setRiskThreshold(int riskThreshold) {
        this.riskThreshold = riskThreshold;
    }

    public boolean isAutoTerminateOnThreshold() {
        return autoTerminateOnThreshold;
    }

    public void setAutoTerminateOnThreshold(boolean autoTerminateOnThreshold) {
        this.autoTerminateOnThreshold = autoTerminateOnThreshold;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
