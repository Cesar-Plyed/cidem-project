package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.application.service.RestrictionService;
import edu.cidemags.integrity_core.legal.domain.entity.RestrictionRequest;
import edu.cidemags.integrity_core.legal.infrastructure.web.dto.LegalRequests;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Derecho de limitacion del tratamiento (Art. 18 GDPR, spec 47). */
@RestController
@RequestMapping("/api/legal/restrict")
public class RestrictionController {

    private final RestrictionService restrictionService;
    private final LegalActorService actors;

    public RestrictionController(RestrictionService restrictionService, LegalActorService actors) {
        this.restrictionService = restrictionService;
        this.actors = actors;
    }

    @PostMapping
    public RestrictionRequest submit(@RequestBody LegalRequests.RestrictionBody body) {
        UUID studentId = actors.resolveStudentId(body.studentId());
        return restrictionService.submitRestriction(studentId, body.restrictionType(), body.reason());
    }

    @DeleteMapping("/{restrictionId}")
    public RestrictionRequest revoke(@PathVariable UUID restrictionId) {
        RestrictionRequest existing = restrictionService.get(restrictionId);
        actors.assertCanActFor(existing.getStudentId());
        return restrictionService.revokeRestriction(restrictionId);
    }

    @GetMapping("/{studentId}/status")
    public LegalViews.RestrictionStatusView status(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return restrictionService.getStatus(studentId);
    }
}
