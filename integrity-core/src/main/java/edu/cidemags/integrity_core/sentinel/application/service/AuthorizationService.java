package edu.cidemags.integrity_core.sentinel.application.service;

import edu.cidemags.integrity_core.sentinel.ports.in.AttemptOwnershipPort;
import edu.cidemags.integrity_core.sentinel.ports.in.ExamOversightAccessPort;
import edu.cidemags.integrity_core.sentinel.ports.out.SentinelDirectoryPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Validacion de permisos de Sentinel.
 *
 * <p>Reglas (spec 10):</p>
 * <ul>
 *   <li>ALUMNO: solo ve eventos/alertas de sus propios intentos.</li>
 *   <li>PROFESOR: solo ve exámenes que administra (created_by_user_id).</li>
 *   <li>ADMINISTRATIVO: acceso total.</li>
 * </ul>
 */
@Service
public class AuthorizationService {

    public static final String ROLE_ALUMNO = "ROLE_ALUMNO";
    public static final String ROLE_PROFESOR = "ROLE_PROFESOR";
    public static final String ROLE_ADMINISTRATIVO = "ROLE_ADMINISTRATIVO";

    private final ExamOversightAccessPort examAccess;
    private final AttemptOwnershipPort attemptOwnership;
    private final SentinelDirectoryPort directory;

    public AuthorizationService(
        ExamOversightAccessPort examAccess,
        AttemptOwnershipPort attemptOwnership,
        SentinelDirectoryPort directory
    ) {
        this.examAccess = examAccess;
        this.attemptOwnership = attemptOwnership;
        this.directory = directory;
    }

    /** UUID del usuario autenticado (el JWT usa el id como principal). */
    public UUID currentUserId() {
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

    public boolean isAdministrativo() {
        return hasRole(ROLE_ADMINISTRATIVO);
    }

    public boolean isProfesor() {
        return hasRole(ROLE_PROFESOR);
    }

    public boolean isAlumno() {
        return hasRole(ROLE_ALUMNO);
    }

    /**
     * GET /sentinel/exams/{examId}/alerts: solo PROFESOR dueno del examen o
     * ADMINISTRATIVO.
     */
    public void assertCanViewExamAlerts(UUID examId) {
        if (examId == null) {
            throw new AccessDeniedException("examId es obligatorio");
        }
        if (isAdministrativo()) {
            return;
        }
        if (isProfesor() && examAccess.canManageExam(examId, currentUserId())) {
            return;
        }
        throw new AccessDeniedException("Sin permiso para ver las alertas de este examen");
    }

    /**
     * GET /sentinel/attempts/{attemptId}/events: ALUMNO dueno del intento,
     * PROFESOR dueno del examen o ADMINISTRATIVO.
     */
    public void assertCanViewAttemptEvents(UUID attemptId) {
        if (attemptId == null) {
            throw new AccessDeniedException("attemptId es obligatorio");
        }
        if (isAdministrativo()) {
            return;
        }

        AttemptOwnershipPort.AttemptOwner owner = attemptOwnership.findOwner(attemptId)
            .orElseThrow(() -> new AccessDeniedException("El intento no existe"));

        UUID userId = currentUserId();

        if (isAlumno() && owner.studentId().equals(userId)) {
            return;
        }
        if (isProfesor() && examAccess.canManageExam(owner.examId(), userId)) {
            return;
        }
        throw new AccessDeniedException("Sin permiso para ver los eventos de este intento");
    }

    /**
     * GET /sentinel/sessions/{sessionId}/alerts: PROFESOR dueno del examen de la
     * sesion o ADMINISTRATIVO. Si la sesion aun no tiene eventos asociados se
     * permite al staff (la respuesta sera una lista vacia).
     */
    public void assertCanViewSessionAlerts(UUID examSessionId) {
        if (examSessionId == null) {
            throw new AccessDeniedException("sessionId es obligatorio");
        }
        if (isAdministrativo()) {
            return;
        }
        if (!isProfesor()) {
            throw new AccessDeniedException("Solo un profesor puede ver las alertas de una sesion");
        }

        Optional<UUID> examId = directory.findExamIdBySession(examSessionId);
        if (examId.isEmpty()) {
            return;
        }
        assertCanViewExamAlerts(examId.get());
    }

    public boolean hasRole(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }
}
