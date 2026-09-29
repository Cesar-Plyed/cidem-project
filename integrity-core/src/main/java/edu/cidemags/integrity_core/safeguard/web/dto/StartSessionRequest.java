package edu.cidemags.integrity_core.safeguard.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartSessionRequest(
        @NotNull UUID examId,
        @NotNull UUID attemptId,
        @Size(max = 512) String userAgent) {}
