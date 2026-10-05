package edu.cidemags.integrity_core.safeguard.application.usecase;

import edu.cidemags.integrity_core.safeguard.application.dto.ConfigView;
import edu.cidemags.integrity_core.safeguard.application.dto.HeartbeatResult;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventResult;
import edu.cidemags.integrity_core.safeguard.application.dto.SessionView;
import edu.cidemags.integrity_core.safeguard.application.dto.StartSessionCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.UpsertConfigCommand;
import edu.cidemags.integrity_core.safeguard.domain.port.ExamAttemptPort;
import edu.cidemags.integrity_core.safeguard.domain.service.SafeguardRiskPolicy;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.domain.event.SafeguardDomainEvent;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardConfigurationRepository;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardEventRepository;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardSessionRepository;
import edu.cidemags.integrity_core.safeguard.domain.exception.SafeguardException;
import edu.cidemags.integrity_core.safeguard.domain.exception.SafeguardException.Reason;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SafeguardService {

    private final SafeguardConfigurationRepository configurationRepository;
    private final SafeguardSessionRepository sessionRepository;
    private final SafeguardEventRepository eventRepository;
    private final ExamAttemptPort examAttemptPort;
    private final SafeguardRiskPolicy riskPolicy;
    private final ApplicationEventPublisher publisher;

    public SafeguardService(SafeguardConfigurationRepository configurationRepository,
                            SafeguardSessionRepository sessionRepository,
                            SafeguardEventRepository eventRepository,
                            ExamAttemptPort examAttemptPort,
                            SafeguardRiskPolicy riskPolicy,
                            ApplicationEventPublisher publisher) {
        this.configurationRepository = configurationRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.examAttemptPort = examAttemptPort;
        this.riskPolicy = riskPolicy;
        this.publisher = publisher;
    }

    // ---------- Use cases ----------

    @Transactional
    public SessionView startSession(StartSessionCommand cmd) {
        if (!examAttemptPort.examExists(cmd.examId())) {
            throw new SafeguardException(Reason.NOT_FOUND, "Exam not found: " + cmd.examId());
        }
        ExamAttemptPort.AttemptRef attempt = examAttemptPort.findAttempt(cmd.attemptId())
                .orElseThrow(() -> new SafeguardException(Reason.NOT_FOUND, "Attempt not found: " + cmd.attemptId()));
        if (!attempt.examId().equals(cmd.examId()) || !attempt.studentId().equals(cmd.studentId())) {
            throw new SafeguardException(Reason.FORBIDDEN, "Attempt does not belong to student/exam");
        }

        SafeguardConfiguration config = configurationRepository.findByExamId(cmd.examId())
                .orElseThrow(() -> new SafeguardException(Reason.NOT_FOUND, "SafeGuard not configured for exam"));
        if (!config.isEnabled()) {
            throw new SafeguardException(Reason.INVALID_STATE, "SafeGuard disabled for exam");
        }

        Optional<SafeguardSession> existing = sessionRepository.findByAttemptId(cmd.attemptId());
        if (existing.isPresent()) {
            return toView(existing.get());
        }

        Instant now = Instant.now();
        SafeguardSession session = new SafeguardSession();
        session.setConfiguration(config);
        session.setExamId(cmd.examId());
        session.setAttemptId(cmd.attemptId());
        session.setStudentId(cmd.studentId());
        session.setStatus(SafeguardSessionStatus.ACTIVE);
        session.setStartedAt(now);
        session.setLastHeartbeatAt(now);
        session.setClientIp(cmd.clientIp());
        session.setUserAgent(truncate(cmd.userAgent(), 512));
        session = sessionRepository.saveAndFlush(session);

        publisher.publishEvent(new SafeguardDomainEvent.SessionStarted(
                session.getId(), session.getExamId(), session.getAttemptId(), session.getStudentId(), now));
        return toView(session);
    }

    @Transactional
    public RecordEventResult recordEvent(RecordEventCommand cmd) {
        SafeguardSession session = loadOwnedForUpdate(cmd.sessionId(), cmd.studentId());

        // Idempotencia estricta: el lock de sesión serializa reintentos concurrentes.
        Optional<SafeguardEvent> existing =
                eventRepository.findBySessionIdAndEventId(session.getId(), cmd.eventId());
        if (existing.isPresent()) {
            SafeguardEvent e = existing.get();
            return new RecordEventResult(e.getId(), e.getEventId(), e.getSeverity(), e.getRiskPoints(),
                    session.getRiskScore(), session.getStatus(), true);
        }

        if (!isLive(session)) {
            throw new SafeguardException(Reason.INVALID_STATE, "Session is not active");
        }

        Instant now = Instant.now();
        Instant occurredAt = (cmd.occurredAt() == null || cmd.occurredAt().isAfter(now)) ? now : cmd.occurredAt();
        SafeguardConfiguration config = session.getConfiguration();

        Long prior = eventRepository.countBySessionIdAndEventType(session.getId(), cmd.eventType());
        SafeguardRiskPolicy.Assessment assessment = riskPolicy.assess(cmd.eventType(), prior, config);

        SafeguardEvent event = new SafeguardEvent();
        event.setSession(session);
        event.setEventId(cmd.eventId());
        event.setEventType(cmd.eventType());
        event.setSeverity(assessment.severity());
        event.setRiskPoints(assessment.riskPoints());
        event.setOccurredAt(occurredAt);
        event.setMetadata(cmd.metadata());
        event = eventRepository.saveAndFlush(event);

        SafeguardSessionStatus previous = session.getStatus();
        session.setRiskScore(session.getRiskScore() + assessment.riskPoints());
        session.setLastHeartbeatAt(now);
        applyThreshold(session, config, now);

        // El dominio es una instantánea desacoplada (entity/domain split): sin este
        // guardado explícito, el score/status/endedAt calculados nunca llegarían a BD.
        sessionRepository.saveAndFlush(session);

        publisher.publishEvent(new SafeguardDomainEvent.RiskEventRecorded(
                session.getId(), session.getExamId(), session.getAttemptId(), session.getStudentId(),
                cmd.eventId(), cmd.eventType(), assessment.severity(), assessment.riskPoints(),
                session.getRiskScore(), occurredAt));

        if (previous != session.getStatus()) {
            publisher.publishEvent(new SafeguardDomainEvent.SessionStatusChanged(
                    session.getId(), session.getExamId(), session.getAttemptId(), session.getStudentId(),
                    previous, session.getStatus(), session.getRiskScore(), now));
        }

        return new RecordEventResult(event.getId(), event.getEventId(), event.getSeverity(),
                event.getRiskPoints(), session.getRiskScore(), session.getStatus(), false);
    }

    @Transactional
    public HeartbeatResult heartbeat(UUID sessionId, UUID studentId) {
        SafeguardSession session = loadOwnedForUpdate(sessionId, studentId);
        Instant now = Instant.now();
        boolean closed = session.getEndedAt() != null;
        if (!closed) {
            session.setLastHeartbeatAt(now);
            sessionRepository.saveAndFlush(session); // instantánea desacoplada: guardado explícito
        }
        return new HeartbeatResult(session.getStatus(), session.getRiskScore(), now, closed);
    }

    @Transactional
    public SessionView closeSession(UUID sessionId, UUID studentId) {
        SafeguardSession session = loadOwnedForUpdate(sessionId, studentId);
        if (session.getEndedAt() != null) {
            return toView(session); // idempotente
        }

        Instant now = Instant.now();
        if (session.getStatus() != SafeguardSessionStatus.FLAGGED) {
            session.setStatus(SafeguardSessionStatus.COMPLETED);
        }
        session.setEndedAt(now);
        session.setLastHeartbeatAt(now);
        // Instantánea desacoplada: sin esto, ended_at/status nunca persistirían.
        sessionRepository.saveAndFlush(session);

        publisher.publishEvent(new SafeguardDomainEvent.SessionClosed(
                session.getId(), session.getExamId(), session.getAttemptId(), session.getStudentId(),
                session.getStatus(), session.getRiskScore(), now));
        return toView(session);
    }

    @Transactional(readOnly = true)
    public ConfigView getConfiguration(UUID examId) {
        SafeguardConfiguration c = configurationRepository.findByExamId(examId)
                .orElseThrow(() -> new SafeguardException(Reason.NOT_FOUND, "SafeGuard not configured for exam: " + examId));
        return toConfigView(c);
    }

    /**
     * Crea o reemplaza la configuración de SafeGuard del examen (upsert).
     * Regla aprobada: solo PROFESOR/ADMINISTRATIVO — la validación de rol vive en el
     * controlador (@PreAuthorize); aquí solo se valida el dominio.
     */
    @Transactional
    public ConfigView upsertConfiguration(UpsertConfigCommand cmd) {
        if (!examAttemptPort.examExists(cmd.examId())) {
            throw new SafeguardException(Reason.NOT_FOUND, "Exam not found: " + cmd.examId());
        }
        // Replican los CHECK constraints de safeguard_configuration (migración V7).
        if (cmd.maxTabSwitches() < 0 || cmd.riskThreshold() <= 0) {
            throw new IllegalArgumentException("Valores de configuración fuera de rango");
        }
        SafeguardConfiguration config = configurationRepository.findByExamId(cmd.examId())
                .orElseGet(SafeguardConfiguration::new);
        config.setExamId(cmd.examId());
        config.setEnabled(cmd.enabled());
        config.setRequireCamera(cmd.requireCamera());
        config.setRequireMicrophone(cmd.requireMicrophone());
        config.setRequireFullscreen(cmd.requireFullscreen());
        config.setBlockCopyPaste(cmd.blockCopyPaste());
        config.setMaxTabSwitches(cmd.maxTabSwitches());
        config.setRiskThreshold(cmd.riskThreshold());
        config.setAutoTerminateOnThreshold(cmd.autoTerminateOnThreshold());
        return toConfigView(configurationRepository.saveAndFlush(config));
    }

    /**
     * Regla aprobada de lectura para ALUMNO: solo puede ver la configuración de un
     * examen en el que tenga un intento (si no, FORBIDDEN).
     */
    @Transactional(readOnly = true)
    public void assertStudentMayReadConfiguration(UUID examId, UUID studentId) {
        if (!examAttemptPort.attemptExistsForStudent(examId, studentId)) {
            throw new SafeguardException(Reason.FORBIDDEN,
                    "Student has no attempt for exam: " + examId);
        }
    }

    private static ConfigView toConfigView(SafeguardConfiguration c) {
        return new ConfigView(c.getExamId(), c.isEnabled(), c.isRequireCamera(), c.isRequireMicrophone(),
                c.isRequireFullscreen(), c.isBlockCopyPaste(), c.getMaxTabSwitches(),
                c.getRiskThreshold(), c.isAutoTerminateOnThreshold());
    }

    // ---------- Internals ----------

    private SafeguardSession loadOwnedForUpdate(UUID sessionId, UUID studentId) {
        SafeguardSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new SafeguardException(Reason.NOT_FOUND, "Session not found: " + sessionId));
        if (!session.getStudentId().equals(studentId)) {
            throw new SafeguardException(Reason.FORBIDDEN, "Session does not belong to student");
        }
        return session;
    }

    /** ACTIVE o FLAGGED (marcada pero aún en curso) y sin cierre. */
    private static boolean isLive(SafeguardSession s) {
        return s.getEndedAt() == null
                && (s.getStatus() == SafeguardSessionStatus.ACTIVE
                || s.getStatus() == SafeguardSessionStatus.FLAGGED);
    }

    private static void applyThreshold(SafeguardSession session, SafeguardConfiguration config, Instant now) {
        if (session.getRiskScore() < config.getRiskThreshold()) {
            return;
        }
        if (config.isAutoTerminateOnThreshold()) {
            session.setStatus(SafeguardSessionStatus.TERMINATED);
            session.setEndedAt(now);
        } else if (session.getStatus() == SafeguardSessionStatus.ACTIVE) {
            session.setStatus(SafeguardSessionStatus.FLAGGED);
        }
    }

    private static SessionView toView(SafeguardSession s) {
        SafeguardConfiguration c = s.getConfiguration();
        return new SessionView(s.getId(), s.getExamId(), s.getAttemptId(), s.getStatus(), s.getRiskScore(),
                s.getStartedAt(), s.getEndedAt(), s.getLastHeartbeatAt(),
                c.isRequireCamera(), c.isRequireMicrophone(), c.isRequireFullscreen(), c.isBlockCopyPaste());
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}