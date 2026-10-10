package edu.cidemags.integrity_core.safeguard.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de dominio puro: sin anotaciones de JPA ni de framework.
 * La representación persistida vive en infrastructure/persistence/entity.
 */
public class SafeguardConfiguration {

    private UUID id;
    private UUID examId;
    private boolean enabled = true;
    private boolean requireCamera = false;
    private boolean requireMicrophone = false;
    private boolean requireFullscreen = false;
    private boolean blockCopyPaste = false;
    private int maxTabSwitches = 3;
    private int riskThreshold = 100;
    private boolean autoTerminateOnThreshold = false;

    /** Requerido por JPA/Hibernate: necesita un constructor sin argumentos. */
    public SafeguardConfiguration() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafeguardConfiguration other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
