package edu.cidemags.integrity_core.sentinel.domain;

import java.util.UUID;

public record ExamSessionId(UUID value) {

    public ExamSessionId {
        if (value == null){
            throw new IllegalArgumentException("examSessionId es obligatorio");
        }
    }
}