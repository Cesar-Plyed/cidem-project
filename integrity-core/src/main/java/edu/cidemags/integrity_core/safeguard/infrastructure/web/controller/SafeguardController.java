package edu.cidemags.integrity_core.safeguard.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventResult;
import edu.cidemags.integrity_core.safeguard.application.dto.StartSessionCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.UpsertConfigCommand;
import edu.cidemags.integrity_core.safeguard.application.usecase.SafeguardService;
import edu.cidemags.integrity_core.safeguard.infrastructure.security.CurrentStudentProvider;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.ConfigResponse;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.HeartbeatRequest;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.HeartbeatResponse;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.RecordEventRequest;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.RecordEventResponse;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.SaveConfigurationRequest;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.SessionResponse;
import edu.cidemags.integrity_core.safeguard.infrastructure.web.dto.StartSessionRequest;

@RestController
@RequestMapping("/api/safeguard")
public class SafeguardController {

    private final SafeguardService service;
    private final CurrentStudentProvider studentProvider;

    public SafeguardController(SafeguardService service, CurrentStudentProvider studentProvider) {
        this.service = service;
        this.studentProvider = studentProvider;
    }

    /**
     * Lectura de la configuración: PROFESOR/ADMINISTRATIVO de cualquier examen;
     * ALUMNO solo si tiene un intento en ese examen (regla aprobada 2026-10-01).
     */
    @GetMapping("/exams/{examId}/config")
    public ConfigResponse getConfig(@PathVariable UUID examId, Authentication authentication) {
        if (!isStaff(authentication)) {
            service.assertStudentMayReadConfiguration(examId, studentProvider.currentStudentId());
        }
        return ConfigResponse.from(service.getConfiguration(examId));
    }

    /** Creación/actualización de restricciones por examen (ToDo Sprint 5). Solo staff. */
    @PutMapping("/exams/{examId}/config")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ConfigResponse upsertConfig(@PathVariable UUID examId,
                                       @Valid @RequestBody SaveConfigurationRequest body) {
        var cmd = new UpsertConfigCommand(examId, body.enabled(), body.requireCamera(),
                body.requireMicrophone(), body.requireFullscreen(), body.blockCopyPaste(),
                body.maxTabSwitches(), body.riskThreshold(), body.autoTerminateOnThreshold());
        return ConfigResponse.from(service.upsertConfiguration(cmd));
    }

    @PostMapping("/sessions/start")
    public SessionResponse start(@Valid @RequestBody StartSessionRequest body, HttpServletRequest request) {
        var cmd = new StartSessionCommand(body.examId(), body.attemptId(), studentProvider.currentStudentId(),
                request.getRemoteAddr(), body.userAgent());
        return SessionResponse.from(service.startSession(cmd));
    }

    @PostMapping("/events")
    public ResponseEntity<RecordEventResponse> recordEvent(@Valid @RequestBody RecordEventRequest body) {
        var cmd = new RecordEventCommand(body.sessionId(), studentProvider.currentStudentId(), body.eventId(),
                body.eventType(), body.occurredAt(), body.metadata());
        RecordEventResult result = service.recordEvent(cmd);
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(RecordEventResponse.from(result));
    }

    @PostMapping("/sessions/{sessionId}/heartbeat")
    public HeartbeatResponse heartbeat(@PathVariable UUID sessionId,
                                       @Valid @RequestBody(required = false) HeartbeatRequest body) {
        return HeartbeatResponse.from(service.heartbeat(sessionId, studentProvider.currentStudentId()));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public SessionResponse close(@PathVariable UUID sessionId) {
        return SessionResponse.from(service.closeSession(sessionId, studentProvider.currentStudentId()));
    }

    /** Mismo patrón de roles por cadena de autoridades que EvaluationReviewController. */
    private static boolean isStaff(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMINISTRATIVO") || a.equals("ROLE_PROFESOR"));
    }
}
