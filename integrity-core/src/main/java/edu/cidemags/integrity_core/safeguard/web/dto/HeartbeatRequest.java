package edu.cidemags.integrity_core.safeguard.web.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record HeartbeatRequest(@NotNull Instant clientTime) {}