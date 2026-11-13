package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.service.AuditService;
import edu.cidemags.integrity_core.legal.domain.entity.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta del audit trail (spec 39). Solo ADMINISTRATIVO. */
@RestController
@RequestMapping("/api/legal/audit")
@PreAuthorize("hasRole('ADMINISTRATIVO')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLog> query(@RequestParam(required = false) UUID actorId,
                                @RequestParam(required = false) String beforeDate,
                                @RequestParam(required = false) String afterDate,
                                @RequestParam(required = false) Integer limit) {
        return auditService.query(actorId, parse(beforeDate, true), parse(afterDate, false), limit);
    }

    /** Acepta ISO-8601 completo o una fecha simple (yyyy-MM-dd). */
    static Instant parse(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException ignored) {
            LocalDate date = LocalDate.parse(value);
            return endOfDay
                    ? date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC)
                    : date.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
    }
}
