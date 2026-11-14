package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.application.service.BreachNotificationService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.domain.entity.DataBreachNotification;
import edu.cidemags.integrity_core.legal.infrastructure.web.dto.LegalRequests;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Notificacion de brechas de datos (Art. 34 GDPR, spec 49). */
@RestController
@RequestMapping("/api/legal/breach")
public class BreachController {

    private final BreachNotificationService breachService;
    private final LegalActorService actors;

    public BreachController(BreachNotificationService breachService, LegalActorService actors) {
        this.breachService = breachService;
        this.actors = actors;
    }

    @PostMapping("/notify")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public DataBreachNotification notifyStudent(@RequestBody LegalRequests.BreachBody body) {
        return breachService.notifyStudent(body.studentId(), body.breachType(), body.description());
    }

    @PutMapping("/{notificationId}/resolve")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
    public DataBreachNotification resolve(@PathVariable UUID notificationId) {
        return breachService.resolveNotification(notificationId);
    }

    @GetMapping("/{studentId}/pending")
    public LegalViews.BreachNotificationList pending(@PathVariable UUID studentId) {
        actors.assertCanActFor(studentId);
        return new LegalViews.BreachNotificationList(breachService.getPendingNotifications(studentId));
    }
}
