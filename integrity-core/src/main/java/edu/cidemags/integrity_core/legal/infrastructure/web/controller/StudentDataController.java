package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.service.AccessService;
import edu.cidemags.integrity_core.legal.application.service.ErasureService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.application.service.PortabilityService;
import edu.cidemags.integrity_core.legal.application.service.RectificationService;
import edu.cidemags.integrity_core.legal.domain.annotation.AuditTrail;
import edu.cidemags.integrity_core.legal.domain.entity.ErasureRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Derechos del interesado sobre sus datos:
 * acceso (42), rectificacion (43), supresion (44) y portabilidad (45).
 */
@RestController
@RequestMapping("/api/legal")
public class StudentDataController {

    private final AccessService accessService;
    private final PortabilityService portabilityService;
    private final RectificationService rectificationService;
    private final ErasureService erasureService;
    private final LegalActorService actors;

    public StudentDataController(AccessService accessService, PortabilityService portabilityService,
                                 RectificationService rectificationService, ErasureService erasureService,
                                 LegalActorService actors) {
        this.accessService = accessService;
        this.portabilityService = portabilityService;
        this.rectificationService = rectificationService;
        this.erasureService = erasureService;
        this.actors = actors;
    }

    @GetMapping("/access/{studentId}")
    public Map<String, Object> access(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return accessService.exportStudentData(studentId);
    }

    @PostMapping("/access/{studentId}/export")
    public ResponseEntity<byte[]> exportAccess(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        byte[] file = accessService.generateExportFile(studentId);
        return download(file, "application/json", "datos-personales-" + studentId + ".json");
    }

    @GetMapping("/portability/{studentId}/json")
    public ResponseEntity<byte[]> portabilityJson(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return download(portabilityService.exportAsJson(studentId), "application/json",
                "portabilidad-" + studentId + ".json");
    }

    @GetMapping("/portability/{studentId}/csv")
    public ResponseEntity<byte[]> portabilityCsv(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return download(portabilityService.exportAsCsv(studentId), "text/csv",
                "portabilidad-" + studentId + ".csv");
    }

    /** Body: {"email": "nuevo@correo.com"} (spec 43). */
    @PutMapping("/data/{entityType}/{entityId}")
    @AuditTrail(action = "RECTIFY_DATA", entityType = "PERSONAL_DATA")
    public Map<String, Object> rectify(@PathVariable String entityType, @PathVariable UUID entityId,
                                       @RequestBody Map<String, Object> fields) {
        assertCanEdit(entityType, entityId);
        Map<String, Object> result = new LinkedHashMap<>();
        fields.forEach((field, value) ->
                result.put(field, rectificationService.rectifyData(entityType, entityId, field, value)));
        return result;
    }

    /** Borrado logico de una entidad (spec 43). */
    @DeleteMapping("/data/{entityType}/{entityId}")
    @AuditTrail(action = "SOFT_DELETE_DATA", entityType = "PERSONAL_DATA")
    public Map<String, Object> softDelete(@PathVariable String entityType, @PathVariable UUID entityId) {
        assertCanEdit(entityType, entityId);
        int updated = rectificationService.softDeleteData(entityType, entityId);
        return Map.of("entityType", entityType, "entityId", entityId, "updated", updated);
    }

    /** Borrado fisico completo (Art. 17, spec 44). */
    @DeleteMapping("/data/{studentId}/full")
    @AuditTrail(action = "ERASE_DATA_HARD", entityType = "STUDENT")
    public ErasureRequest eraseFull(@PathVariable @NotNull UUID studentId) {
        actors.assertCanActFor(studentId);
        return erasureService.eraseStudentData(studentId);
    }

    /** Borrado logico completo (Art. 17, spec 44). */
    @DeleteMapping("/data/{studentId}/soft")
    @AuditTrail(action = "ERASE_DATA_SOFT", entityType = "STUDENT")
    public ErasureRequest eraseSoft(@PathVariable @NotNull UUID studentId) {
        actors.assertCanActFor(studentId);
        return erasureService.softEraseStudentData(studentId);
    }

    private void assertCanEdit(String entityType, UUID entityId) {
        if (actors.isStaff()) {
            return;
        }
        boolean ownProfile = ("USER".equalsIgnoreCase(entityType)
                || "STUDENT".equalsIgnoreCase(entityType)
                || "EMAIL".equalsIgnoreCase(entityType))
                && actors.currentActorId().equals(entityId);
        if (!ownProfile) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo puedes rectificar tus propios datos");
        }
    }

    private static ResponseEntity<byte[]> download(byte[] body, String contentType, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
