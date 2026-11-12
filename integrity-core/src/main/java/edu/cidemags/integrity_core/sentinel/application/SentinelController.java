package edu.cidemags.integrity_core.sentinel.application;

import edu.cidemags.integrity_core.sentinel.application.dto.AlertView;
import edu.cidemags.integrity_core.sentinel.application.service.AuthorizationService;
import edu.cidemags.integrity_core.sentinel.application.service.SentinelQueryService;
import edu.cidemags.integrity_core.sentinel.infrastructure.adapter.AlertResponseDTO;
import edu.cidemags.integrity_core.sentinel.infrastructure.adapter.SentinelAlertEventDTO;
import edu.cidemags.integrity_core.sentinel.infrastructure.adapter.SentinelEventResponse;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventCommand;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints REST de Sentinel: recibir eventos y consultar alertas/eventos por
 * examen, intento y sesion.
 *
 * <p>El rol se valida con {@code @PreAuthorize} y la propiedad del recurso con
 * {@link AuthorizationService} (spec 10).</p>
 */
@RestController
@RequestMapping("/sentinel")
@Tag(name = "Sentinel", description = "Gestión de alertas y eventos de seguridad")
public class SentinelController {

    private final ReceiveSuspiciousEventUseCase receiveSuspiciousEventUseCase;
    private final SentinelQueryService queryService;
    private final AuthorizationService authorization;

    public SentinelController(
        ReceiveSuspiciousEventUseCase receiveSuspiciousEventUseCase,
        SentinelQueryService queryService,
        AuthorizationService authorization
    ) {
        this.receiveSuspiciousEventUseCase = receiveSuspiciousEventUseCase;
        this.queryService = queryService;
        this.authorization = authorization;
    }

    @Operation(summary = "Recibir evento de alerta", description = "Registra un evento sospechoso y evalúa si escala a alerta.")
    @ApiResponse(responseCode = "201", description = "Evento creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Sin permiso sobre el intento")
    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR', 'ADMINISTRATIVO')")
    public ResponseEntity<UUID> receiveEvent(@Valid @RequestBody SentinelAlertEventDTO eventDTO) {
        // Un alumno no puede atribuir eventos a un intento ajeno ni suplantar a
        // otro alumno: su id sale del JWT, nunca del body.
        UUID studentId = authorization.isAlumno()
            ? authorization.currentUserId()
            : (eventDTO.studentId() != null ? eventDTO.studentId() : authorization.currentUserId());

        authorization.assertCanViewAttemptEvents(eventDTO.attemptId());

        UUID eventId = eventDTO.eventId() != null ? eventDTO.eventId() : UUID.randomUUID();
        UUID examSessionId = eventDTO.examSessionId() != null
            ? eventDTO.examSessionId()
            : eventDTO.attemptId();
        Instant occurredAt = eventDTO.occurredAt() != null ? eventDTO.occurredAt() : Instant.now();

        receiveSuspiciousEventUseCase.receive(new ReceiveSuspiciousEventCommand(
            examSessionId,
            studentId,
            eventDTO.attemptId(),
            eventDTO.eventType(),
            occurredAt,
            eventDTO.examId(),
            eventDTO.source() == null ? "SENTINEL" : eventDTO.source(),
            eventDTO.payload()
        ));

        return ResponseEntity.status(201).body(eventId);
    }

    @Operation(summary = "Consultar alertas por examen", description = "Lista alertas filtradas por severidad y rango de fechas.")
    @ApiResponse(responseCode = "200", description = "Lista de alertas")
    @GetMapping("/exams/{examId}/alerts")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMINISTRATIVO')")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByExam(
            @Parameter(description = "ID del examen") @PathVariable UUID examId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        authorization.assertCanViewExamAlerts(examId);
        return ResponseEntity.ok(queryService.findAlertsByExam(examId, severity, from, to).stream()
            .map(AlertResponseDTO::from)
            .toList());
    }

    @Operation(summary = "Consultar eventos por intento", description = "Lista eventos de un intento, ordenados por fecha descendente.")
    @ApiResponse(responseCode = "200", description = "Lista de eventos")
    @GetMapping("/attempts/{attemptId}/events")
    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR', 'ADMINISTRATIVO')")
    public ResponseEntity<List<SentinelEventResponse>> getEventsByAttempt(
            @Parameter(description = "ID del intento") @PathVariable UUID attemptId,
            @Min(1) @Max(200) @RequestParam(defaultValue = "50") int limit) {
        authorization.assertCanViewAttemptEvents(attemptId);
        return ResponseEntity.ok(queryService.findEventsByAttempt(attemptId, limit).stream()
            .map(SentinelEventResponse::from)
            .toList());
    }

    @Operation(summary = "Consultar alertas por sesión", description = "Lista alertas filtradas por severidad y rango de fechas.")
    @ApiResponse(responseCode = "200", description = "Lista de alertas")
    @GetMapping("/sessions/{sessionId}/alerts")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMINISTRATIVO')")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsBySession(
            @Parameter(description = "ID de sesión") @PathVariable UUID sessionId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        authorization.assertCanViewSessionAlerts(sessionId);
        return ResponseEntity.ok(queryService.findAlertsBySession(sessionId, severity, from, to).stream()
            .map(AlertResponseDTO::from)
            .toList());
    }

    @Operation(summary = "Resolver alerta", description = "Marca una alerta como resuelta.")
    @ApiResponse(responseCode = "204", description = "Alerta resuelta exitosamente")
    @ApiResponse(responseCode = "404", description = "Alerta no encontrada")
    @PutMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Void> resolveAlert(
            @Parameter(description = "ID de la alerta") @PathVariable UUID alertId) {
        AlertView alert = queryService.findAlertById(alertId).orElse(null);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        if (alert.examId() != null) {
            authorization.assertCanViewExamAlerts(alert.examId());
        }
        queryService.resolveAlert(alertId);
        return ResponseEntity.noContent().build();
    }
}
