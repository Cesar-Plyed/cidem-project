package edu.cidemags.integrity_core.safeguard.web;

import edu.cidemags.integrity_core.safeguard.service.SafeguardService;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.RecordEventResult;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.StartSessionCommand;
import edu.cidemags.integrity_core.safeguard.web.dto.ConfigResponse;
import edu.cidemags.integrity_core.safeguard.web.dto.HeartbeatRequest;
import edu.cidemags.integrity_core.safeguard.web.dto.HeartbeatResponse;
import edu.cidemags.integrity_core.safeguard.web.dto.RecordEventRequest;
import edu.cidemags.integrity_core.safeguard.web.dto.RecordEventResponse;
import edu.cidemags.integrity_core.safeguard.web.dto.SessionResponse;
import edu.cidemags.integrity_core.safeguard.web.dto.StartSessionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/safeguard")
public class SafeguardController {

    private final SafeguardService service;
    private final CurrentStudentProvider studentProvider;

    public SafeguardController(SafeguardService service, CurrentStudentProvider studentProvider) {
        this.service = service;
        this.studentProvider = studentProvider;
    }

    @GetMapping("/config/{examId}")
    public ConfigResponse getConfig(@PathVariable UUID examId) {
        return ConfigResponse.from(service.getConfiguration(examId));
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
}