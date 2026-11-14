package edu.cidemags.integrity_core.legal.application.service;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.model.ActorRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Resuelve el actor autenticado y aplica las reglas de acceso a datos propios. */
@Service
public class LegalActorService {

    public UUID currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No hay usuario autenticado");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("El principal autenticado no es un UUID", ex);
        }
    }

    public ActorRole currentActorRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ActorRole.SYSTEM;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMINISTRATIVO".equals(role)) {
                return ActorRole.ADMIN;
            }
            if ("ROLE_PROFESOR".equals(role)) {
                return ActorRole.PROFESSOR;
            }
            if ("ROLE_ALUMNO".equals(role)) {
                return ActorRole.STUDENT;
            }
        }
        return ActorRole.SYSTEM;
    }

    public boolean isStaff() {
        ActorRole role = currentActorRole();
        return role == ActorRole.ADMIN || role == ActorRole.PROFESSOR;
    }

    /** Un alumno solo puede operar sobre sus propios datos; el staff, sobre cualquiera. */
    public void assertCanActFor(UUID studentId) {
        UUID actorId = currentActorId();
        if (isStaff()) {
            return;
        }
        if (!actorId.equals(studentId)) {
            throw new AccessDeniedException("Solo puedes acceder a tus propios datos personales");
        }
    }

    public UUID resolveStudentId(UUID requested) {
        UUID studentId = requested == null ? currentActorId() : requested;
        assertCanActFor(studentId);
        return studentId;
    }
}
