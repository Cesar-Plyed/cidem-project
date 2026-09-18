package edu.cidemags.integrity_core.sentinel.application.event;

import edu.cidemags.integrity_core.sentinel.domain.Alert;

public record AlertGeneratedEvent(Alert alert) {
} 