package edu.cidemags.integrity_core.auth.application.dto;

public record LoginCommand(String email, String rawPassword) {
}
