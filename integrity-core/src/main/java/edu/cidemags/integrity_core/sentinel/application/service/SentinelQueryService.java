package edu.cidemags.integrity_core.sentinel.application.service;

import edu.cidemags.integrity_core.sentinel.application.dto.AlertView;
import edu.cidemags.integrity_core.sentinel.application.dto.SentinelEventView;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import edu.cidemags.integrity_core.sentinel.ports.out.SentinelDirectoryPort;
import edu.cidemags.integrity_core.sentinel.ports.out.SuspiciousEventRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Consultas de lectura de Sentinel para la API y la pantalla de alertas. */
@Service
public class SentinelQueryService {

    private final AlertRepositoryPort alertRepository;
    private final SuspiciousEventRepositoryPort eventRepository;
    private final SentinelDirectoryPort directory;

    public SentinelQueryService(
        AlertRepositoryPort alertRepository,
        SuspiciousEventRepositoryPort eventRepository,
        SentinelDirectoryPort directory
    ) {
        this.alertRepository = alertRepository;
        this.eventRepository = eventRepository;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public List<AlertView> findAlertsByExam(UUID examId, String severity, Instant from, Instant to) {
        return alertRepository.findByExamId(examId, severity, from, to).stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertView> findAlertsBySession(
        UUID examSessionId, String severity, Instant from, Instant to
    ) {
        AlertSeverity filter = parseSeverity(severity);
        return alertRepository.findByExamSessionId(new ExamSessionId(examSessionId)).stream()
            .filter(alert -> filter == null || alert.severity() == filter)
            .filter(alert -> from == null || !alert.generatedAt().isBefore(from))
            .filter(alert -> to == null || !alert.generatedAt().isAfter(to))
            .sorted(Comparator.comparing(Alert::generatedAt).reversed())
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertView> findAlertsByAttempt(UUID attemptId, int limit) {
        return alertRepository.findByAttemptId(attemptId, limit).stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SentinelEventView> findEventsByAttempt(UUID attemptId, int limit) {
        return eventRepository.findByAttemptId(attemptId, limit).stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<AlertView> findAlertById(UUID alertId) {
        return alertRepository.findById(alertId).map(this::toView);
    }

    @Transactional
    public boolean resolveAlert(UUID alertId) {
        return alertRepository.markResolved(alertId);
    }

    @Transactional(readOnly = true)
    public UUID resolveExamIdForSession(UUID examSessionId) {
        return directory.findExamIdBySession(examSessionId).orElse(null);
    }

    private AlertView toView(Alert alert) {
        UUID studentId = alert.studentId().value();
        UUID examId = alert.examId();
        return new AlertView(
            alert.id(),
            examId,
            alert.attemptId().value(),
            alert.examSessionId().value(),
            studentId,
            directory.findStudentName(studentId).orElse(null),
            examId == null ? null : directory.findExamName(examId).orElse(null),
            alert.alertType(),
            alert.severity(),
            alert.reason(),
            alert.generatedAt(),
            alert.resolved()
        );
    }

    private SentinelEventView toView(SuspiciousEvent event) {
        return new SentinelEventView(
            event.id(),
            event.id().toString(),
            event.examId(),
            event.examSessionId().value(),
            event.studentId().value(),
            event.attemptId().value(),
            event.type().name(),
            event.source(),
            event.payload(),
            event.occurredAt()
        );
    }

    private static AlertSeverity parseSeverity(String severity) {
        if (severity == null || severity.isBlank() || "all".equalsIgnoreCase(severity)) {
            return null;
        }
        try {
            return AlertSeverity.valueOf(severity.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Severidad invalida: " + severity, ex);
        }
    }
}
