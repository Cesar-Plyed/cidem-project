package edu.cidemags.integrity_core.safeguard.service;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.event.SafeguardDomainEvent;
import edu.cidemags.integrity_core.safeguard.repository.SafeguardConfigurationRepository;
import edu.cidemags.integrity_core.safeguard.repository.SafeguardEventRepository;
import edu.cidemags.integrity_core.safeguard.repository.SafeguardSessionRepository;
import edu.cidemags.integrity_core.safeguard.service.SafeguardException.Reason;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class SafeguardService {

    private static final int MAX_METADATA_LENGTH = 4000;

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

    // ---------- Commands / Results ----------

    public record StartSessionCommand(UUID examId, UUID attemptId, UUID studentId,
                                  String clientIp, String userAgent) {
        public StartSessionCommand {
            Objects.requireNonNull(examId, "examId");
            Objects.requireNonNull(attemptId, "attemptId");
            Objects.requireNonNull(studentId, "studentId");
        }
    }

    public record RecordEventCommand(UUID sessionId, UUID studentId, UUID eventId,
                                 SafeguardEventType eventType, Instant occurredAt, String metadata) {
        public RecordEventCommand {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(studentId, "studentId");
            Objects.requireNonNull(eventId, "eventId");
            Objects.requireNonNull(eventType, "eventType");
            if (metadata != null && metadata.length() > MAX_METADATA_LENGTH) {
                throw new IllegalArgumentException("metadata too large");
            }
        }
    }

    public record ConfigView(UUID examId, boolean enabled, boolean requireCamera, boolean requireMicrophone,
                         boolean requireFullscreen, boolean blockCopyPaste, int maxTabSwitches) {}

    public record SessionView(UUID sessionId, UUID examId, UUID attemptId,
                          SafeguardSessionStatus status, int riskScore,
                          Instant startedAt, Instant endedAt, Instant lastHeartbeatAt,
                          boolean requireCamera, boolean requireMicrophone,
                          boolean requireFullscreen, boolean blockCopyPaste) {}

    public record RecordEventResult(UUID id, UUID eventId, int severity, int riskPoints,
                                    int totalRiskScore, SafeguardSessionStatus sessionStatus,
                                    boolean duplicate) {}

    public record HeartbeatResult(SafeguardSessionStatus status, int riskScore,
                                  Instant serverTime, boolean closed) {}

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

        publisher.publishEvent(new SafeguardDomainEvent.SessionClosed(
                session.getId(), session.getExamId(), session.getAttemptId(), session.getStudentId(),
                session.getStatus(), session.getRiskScore(), now));
        return toView(session);
    }

    @Transactional(readOnly = true)
    public ConfigView getConfiguration(UUID examId) {
        SafeguardConfiguration c = configurationRepository.findByExamId(examId)
                .orElseThrow(() -> new SafeguardException(Reason.NOT_FOUND, "SafeGuard not configured for exam: " + examId));
        return new ConfigView(c.getExamId(), c.isEnabled(), c.isRequireCamera(), c.isRequireMicrophone(),
                c.isRequireFullscreen(), c.isBlockCopyPaste(), c.getMaxTabSwitches());
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