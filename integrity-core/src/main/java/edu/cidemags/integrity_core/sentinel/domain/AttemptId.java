package edu.cidemags.integrity_core.sentinel.domain;

import java.util.UUID;

public record AttemptId(UUID value) {

    public AttemptId {
        if (value == null) {
            throw new IllegalArgumentException("attemptId es obligatorio");
        }
    }
}