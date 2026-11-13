package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.application.service.DPIAService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.domain.entity.Dpia;
import edu.cidemags.integrity_core.legal.infrastructure.web.dto.LegalRequests;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Evaluacion de impacto en proteccion de datos (Art. 35 GDPR, spec 50). */
@RestController
@RequestMapping("/api/legal/dpia")
public class DPIAController {

    private final DPIAService dpiaService;
    private final LegalActorService actors;

    public DPIAController(DPIAService dpiaService, LegalActorService actors) {
        this.dpiaService = dpiaService;
        this.actors = actors;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public Dpia create(@RequestBody LegalRequests.DpiaBody body) {
        return dpiaService.createDPIA(body.title(), body.description(), body.riskLevel(),
                body.mitigationMeasures());
    }

    @PutMapping("/{dpiId}/review")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public Dpia review(@PathVariable UUID dpiId, @RequestBody LegalRequests.DpiaReviewBody body) {
        UUID reviewedBy = body.reviewedBy() == null ? actors.currentActorId() : body.reviewedBy();
        return dpiaService.reviewDPIA(dpiId, reviewedBy, body.status());
    }

    /**
     * El spec 50 usa {studentId} en la ruta, pero la EIPD no pertenece a un
     * alumno: solo el staff puede consultar las pendientes de revision.
     */
    @GetMapping("/{studentId}/pending")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public LegalViews.DpiaList pending(@PathVariable UUID studentId) {
        return dpiaService.getPendingReviewView();
    }

    @GetMapping("/{dpiId}/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID dpiId) {
        byte[] pdf = dpiaService.exportPDF(dpiId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"eipd-" + dpiId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
