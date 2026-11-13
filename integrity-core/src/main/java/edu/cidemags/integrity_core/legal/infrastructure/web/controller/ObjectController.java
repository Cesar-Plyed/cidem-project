package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.application.service.ObjectService;
import edu.cidemags.integrity_core.legal.domain.entity.ObjectRequest;
import edu.cidemags.integrity_core.legal.infrastructure.web.dto.LegalRequests;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Derecho de objecion (Art. 21 GDPR, spec 46). */
@RestController
@RequestMapping("/api/legal/object")
public class ObjectController {

    private final ObjectService objectService;
    private final LegalActorService actors;

    public ObjectController(ObjectService objectService, LegalActorService actors) {
        this.objectService = objectService;
        this.actors = actors;
    }

    @PostMapping
    public ObjectRequest submit(@RequestBody LegalRequests.ObjectRequestBody body) {
        UUID studentId = actors.resolveStudentId(body.studentId());
        return objectService.submitObject(studentId, body.objectType(), body.reason());
    }

    @PutMapping("/{objectId}/resolve")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public ObjectRequest resolve(@PathVariable UUID objectId,
                                 @RequestBody LegalRequests.ObjectResolveBody body) {
        return objectService.resolveObject(objectId, body.status(), body.resolutionNote());
    }

    @GetMapping("/{studentId}/status")
    public LegalViews.ObjectStatusView status(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return objectService.getStatus(studentId);
    }

    @GetMapping("/{studentId}/history")
    public Map<String, Object> history(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return Map.of("requests", objectService.history(studentId));
    }
}
