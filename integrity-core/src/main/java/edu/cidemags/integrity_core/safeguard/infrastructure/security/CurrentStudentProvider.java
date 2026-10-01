package edu.cidemags.integrity_core.safeguard.infrastructure.security;

import java.util.UUID;

public interface CurrentStudentProvider {
    UUID currentStudentId();
}