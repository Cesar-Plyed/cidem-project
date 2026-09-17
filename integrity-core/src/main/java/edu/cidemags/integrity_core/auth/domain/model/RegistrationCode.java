package edu.cidemags.integrity_core.auth.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class RegistrationCode {

    private UUID id;
    private final String code;
    private final Role role;
    private boolean used;
    private final OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;

    public RegistrationCode(String code, Role role, OffsetDateTime expiresAt) {
        this.code = code;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public RegistrationCode(UUID id, String code, Role role, boolean used, OffsetDateTime expiresAt,
            OffsetDateTime usedAt) {
        validate(code, role);
        this.id = id;
        this.code = code;
        this.role = role;
        this.used = used;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }
    
    private void validate(String code, Role role) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El codigo de registro no puede estar vacio");
        }

        if (role == null || role == Role.ALUMNO) {
            throw new IllegalArgumentException(
                    "El codigo de registro solo aplica a roles elevados como Administrador u Profesor");
        }
    }

    public boolean isExpired(){
        return expiresAt != null && OffsetDateTime.now(ZoneId.systemDefault()).isAfter(expiresAt);
    }

    public boolean isValid(){
        return !used && !isExpired();
    }

    public void markUsed(){
        this.used = true;
        this.usedAt = OffsetDateTime.now(ZoneId.systemDefault());
    }

    public UUID getId() {
        return id;
    }


    public String getCode() {
        return code;
    }

    public Role getRole() {
        return role;
    }

    public boolean isUsed() {
        return used;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    } 

}
