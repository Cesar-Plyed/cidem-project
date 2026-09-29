package edu.cidemags.integrity_core.safeguard.service;

public class SafeguardException extends RuntimeException {

    public enum Reason { NOT_FOUND, FORBIDDEN, INVALID_STATE }

    private final Reason reason;

    public SafeguardException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
