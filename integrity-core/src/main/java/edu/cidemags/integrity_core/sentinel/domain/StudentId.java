package edu.cidemags.integrity_core.sentinel.domain;

import java.util.UUID;

public record StudentId(UUID value) {
    public StudentId{
        if (value == null ){
            throw new IllegalArgumentException("studentId es obligatorio");
        }
    }
}