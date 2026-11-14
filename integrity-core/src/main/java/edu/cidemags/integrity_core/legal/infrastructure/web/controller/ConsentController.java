package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.application.service.ConsentService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.domain.entity.ConsentLog;
import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import edu.cidemags.integrity_core.legal.infrastructure.web.dto.LegalRequests;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consentimiento informado y su revocacion (specs 36, 40 y 48). */
@RestController
@RequestMapping("/api/legal/consent")
public class ConsentController {

    private final ConsentService consentService;
    private final LegalActorService actors;

    public ConsentController(ConsentService consentService, LegalActorService actors) {
        this.consentService = consentService;
        this.actors = actors;
    }

    @PostMapping("/grant")
    public ResponseEntity<LegalViews.ConsentStatus> grant(@RequestBody LegalRequests.ConsentRequest body,
                                                          HttpServletRequest request) {
        UUID studentId = actors.resolveStudentId(body.studentId());
        ConsentLog log = consentService.grantConsent(studentId, body.consentType(),
                clientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(toStatus(log));
    }

    @PostMapping("/revoke")
    public LegalViews.ConsentStatus revoke(@RequestBody LegalRequests.ConsentRequest body,
                                           HttpServletRequest request) {
        UUID studentId = actors.resolveStudentId(body.studentId());
        return toStatus(consentService.revokeConsent(studentId, body.consentType(),
                clientIp(request), request.getHeader("User-Agent")));
    }

    /** Revoca todos los consentimientos vigentes del alumno (spec 40). */
    @PostMapping("/revoke-all")
    public List<LegalViews.ConsentStatus> revokeAll(
            @RequestBody(required = false) LegalRequests.ConsentRevokeAllRequest body,
            HttpServletRequest request) {
        UUID studentId = actors.resolveStudentId(body == null ? null : body.studentId());
        return consentService.revokeAll(studentId, clientIp(request), request.getHeader("User-Agent"))
                .stream().map(ConsentController::toStatus).toList();
    }

    /** Revoca un tipo concreto (POST, spec 40). */
    @PostMapping("/revoke/{type}")
    public LegalViews.ConsentStatus revokeByType(@PathVariable ConsentType type,
                                                 @RequestParam(required = false) UUID studentId,
                                                 HttpServletRequest request) {
        return revoke(new LegalRequests.ConsentRequest(studentId, type), request);
    }

    /** Alias REST de POST /revoke/{type} (spec 40). */
    @DeleteMapping("/{type}")
    public LegalViews.ConsentStatus deleteByType(@PathVariable ConsentType type,
                                                 @RequestParam(required = false) UUID studentId,
                                                 HttpServletRequest request) {
        return revokeByType(type, studentId, request);
    }

    @GetMapping("/{studentId}/status")
    public LegalViews.ConsentOverview status(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return consentService.getStatus(studentId);
    }

    /** Estado completo de consentimientos (spec 48). */
    @GetMapping("/{studentId}")
    public LegalViews.ConsentOverview detail(@PathVariable UUID studentId) {
        return status(studentId);
    }

    /** Historial append-only de cambios (spec 48). */
    @GetMapping("/history")
    public LegalViews.ConsentHistory history(@RequestParam(required = false) UUID studentId) {
        return consentService.getHistory(actors.resolveStudentId(studentId));
    }

    private static LegalViews.ConsentStatus toStatus(ConsentLog log) {
        return new LegalViews.ConsentStatus(log.getId(), log.getConsentType().name(),
                log.isGranted() ? "granted" : "revoked", log.getGrantedAt(), log.getRevokedAt());
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
