package edu.cidemags.integrity_core.safeguard.application.usecase;
import edu.cidemags.integrity_core.safeguard.domain.port.ExamAttemptPort;
import edu.cidemags.integrity_core.safeguard.domain.service.SafeguardRiskPolicy;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.domain.event.SafeguardDomainEvent;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardConfigurationRepository;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardEventRepository;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardSessionRepository;
import edu.cidemags.integrity_core.safeguard.domain.exception.SafeguardException;
import edu.cidemags.integrity_core.safeguard.domain.exception.SafeguardException.Reason;
import edu.cidemags.integrity_core.safeguard.application.dto.HeartbeatResult;
import edu.cidemags.integrity_core.safeguard.application.dto.ConfigView;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventResult;
import edu.cidemags.integrity_core.safeguard.application.dto.SessionView;
import edu.cidemags.integrity_core.safeguard.application.dto.StartSessionCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.UpsertConfigCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafeguardServiceTest {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Mock private SafeguardConfigurationRepository configurationRepository;
    @Mock private SafeguardSessionRepository sessionRepository;
    @Mock private SafeguardEventRepository eventRepository;
    @Mock private ExamAttemptPort examAttemptPort;
    @Mock private ApplicationEventPublisher publisher;

    private SafeguardService service;
    private SafeguardConfiguration config;

    @BeforeEach
    void setUp() {
        service = new SafeguardService(configurationRepository, sessionRepository, eventRepository,
                examAttemptPort, new SafeguardRiskPolicy(), publisher);

        config = new SafeguardConfiguration();
        ReflectionTestUtils.setField(config, "id", UUID.randomUUID());
        config.setExamId(EXAM_ID);
        config.setEnabled(true);
        config.setMaxTabSwitches(3);
        config.setRiskThreshold(100);
        config.setAutoTerminateOnThreshold(false);
    }

    // ---------- helpers ----------

    private SafeguardSession session(SafeguardSessionStatus status) {
        SafeguardSession s = new SafeguardSession();
        ReflectionTestUtils.setField(s, "id", SESSION_ID);
        s.setConfiguration(config);
        s.setExamId(EXAM_ID);
        s.setAttemptId(ATTEMPT_ID);
        s.setStudentId(STUDENT_ID);
        s.setStatus(status);
        s.setStartedAt(Instant.now().minusSeconds(60));
        s.setLastHeartbeatAt(Instant.now().minusSeconds(30));
        return s;
    }

    private RecordEventCommand eventCmd(UUID eventId, SafeguardEventType type) {
        return new RecordEventCommand(SESSION_ID, STUDENT_ID, eventId, type, Instant.now(), null);
    }

    private StartSessionCommand startCmd() {
        return new StartSessionCommand(EXAM_ID, ATTEMPT_ID, STUDENT_ID, "127.0.0.1", "JUnit-UA");
    }

    private List<Object> capturePublished(int expected) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(expected)).publishEvent(captor.capture());
        return captor.getAllValues();
    }

    // ---------- startSession ----------

    @Test
    void startSession_success_persistsActiveSessionAndPublishesEvent() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(examAttemptPort.findAttempt(ATTEMPT_ID))
                .thenReturn(Optional.of(new ExamAttemptPort.AttemptRef(ATTEMPT_ID, EXAM_ID, STUDENT_ID)));
        when(configurationRepository.findByExamId(EXAM_ID)).thenReturn(Optional.of(config));
        when(sessionRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(Optional.empty());
        when(sessionRepository.saveAndFlush(any(SafeguardSession.class))).thenAnswer(inv -> {
            SafeguardSession s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", SESSION_ID);
            return s;
        });

        SessionView view = service.startSession(startCmd());

        ArgumentCaptor<SafeguardSession> saved = ArgumentCaptor.forClass(SafeguardSession.class);
        verify(sessionRepository).saveAndFlush(saved.capture());
        SafeguardSession s = saved.getValue();
        assertEquals(SafeguardSessionStatus.ACTIVE, s.getStatus());
        assertEquals(EXAM_ID, s.getExamId());
        assertEquals(ATTEMPT_ID, s.getAttemptId());
        assertEquals(STUDENT_ID, s.getStudentId());
        assertEquals("127.0.0.1", s.getClientIp());
        assertNotNull(s.getStartedAt());
        assertNotNull(s.getLastHeartbeatAt());

        assertEquals(SESSION_ID, view.sessionId());
        assertEquals(SafeguardSessionStatus.ACTIVE, view.status());

        SafeguardDomainEvent.SessionStarted evt = assertInstanceOf(
                SafeguardDomainEvent.SessionStarted.class, capturePublished(1).get(0));
        assertEquals(SESSION_ID, evt.sessionId());
        assertEquals(EXAM_ID, evt.examId());
        assertEquals(ATTEMPT_ID, evt.attemptId());
        assertEquals(STUDENT_ID, evt.studentId());
    }

    @Test
    void startSession_examNotFound_throwsNotFound() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(false);

        SafeguardException ex = assertThrows(SafeguardException.class, () -> service.startSession(startCmd()));

        assertEquals(Reason.NOT_FOUND, ex.getReason());
        verify(examAttemptPort, never()).findAttempt(any());
        verifyNoInteractions(sessionRepository, publisher);
    }

    @Test
    void startSession_attemptNotFound_throwsNotFound() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(examAttemptPort.findAttempt(ATTEMPT_ID)).thenReturn(Optional.empty());

        SafeguardException ex = assertThrows(SafeguardException.class, () -> service.startSession(startCmd()));

        assertEquals(Reason.NOT_FOUND, ex.getReason());
        verifyNoInteractions(sessionRepository, publisher);
    }

    @Test
    void startSession_attemptBelongsToAnotherStudent_throwsForbidden() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(examAttemptPort.findAttempt(ATTEMPT_ID))
                .thenReturn(Optional.of(new ExamAttemptPort.AttemptRef(ATTEMPT_ID, EXAM_ID, UUID.randomUUID())));

        SafeguardException ex = assertThrows(SafeguardException.class, () -> service.startSession(startCmd()));

        assertEquals(Reason.FORBIDDEN, ex.getReason());
        verify(sessionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void startSession_attemptBelongsToAnotherExam_throwsForbidden() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(examAttemptPort.findAttempt(ATTEMPT_ID))
                .thenReturn(Optional.of(new ExamAttemptPort.AttemptRef(ATTEMPT_ID, UUID.randomUUID(), STUDENT_ID)));

        SafeguardException ex = assertThrows(SafeguardException.class, () -> service.startSession(startCmd()));

        assertEquals(Reason.FORBIDDEN, ex.getReason());
        verifyNoInteractions(publisher);
    }

    @Test
    void startSession_configurationDisabled_throwsInvalidState() {
        config.setEnabled(false);
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(examAttemptPort.findAttempt(ATTEMPT_ID))
                .thenReturn(Optional.of(new ExamAttemptPort.AttemptRef(ATTEMPT_ID, EXAM_ID, STUDENT_ID)));
        when(configurationRepository.findByExamId(EXAM_ID)).thenReturn(Optional.of(config));

        SafeguardException ex = assertThrows(SafeguardException.class, () -> service.startSession(startCmd()));

        assertEquals(Reason.INVALID_STATE, ex.getReason());
        verifyNoInteractions(publisher);
    }

    @Test
    void startSession_sessionAlreadyExists_returnsExistingWithoutSavingOrPublishing() {
        SafeguardSession existing = session(SafeguardSessionStatus.ACTIVE);
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(examAttemptPort.findAttempt(ATTEMPT_ID))
                .thenReturn(Optional.of(new ExamAttemptPort.AttemptRef(ATTEMPT_ID, EXAM_ID, STUDENT_ID)));
        when(configurationRepository.findByExamId(EXAM_ID)).thenReturn(Optional.of(config));
        when(sessionRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(Optional.of(existing));

        SessionView view = service.startSession(startCmd());

        assertEquals(SESSION_ID, view.sessionId());
        verify(sessionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);
    }

    // ---------- recordEvent ----------

    @Test
    void recordEvent_success_calculatesSeverityUpdatesScoreAndPublishes() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        UUID eventId = UUID.randomUUID();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.TAB_SWITCH)).thenReturn(0L);
        when(eventRepository.saveAndFlush(any(SafeguardEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.TAB_SWITCH));

        // TAB_SWITCH dentro del límite => severidad 2, 5 puntos
        assertFalse(result.duplicate());
        assertEquals(2, result.severity());
        assertEquals(5, result.riskPoints());
        assertEquals(5, result.totalRiskScore());
        assertEquals(SafeguardSessionStatus.ACTIVE, result.sessionStatus());
        assertEquals(5, session.getRiskScore());

        ArgumentCaptor<SafeguardEvent> saved = ArgumentCaptor.forClass(SafeguardEvent.class);
        verify(eventRepository).saveAndFlush(saved.capture());
        assertEquals(eventId, saved.getValue().getEventId());
        assertEquals(SafeguardEventType.TAB_SWITCH, saved.getValue().getEventType());
        assertSame(session, saved.getValue().getSession());

        SafeguardDomainEvent.RiskEventRecorded evt = assertInstanceOf(
                SafeguardDomainEvent.RiskEventRecorded.class, capturePublished(1).get(0));
        assertEquals(eventId, evt.eventId());
        assertEquals(SafeguardEventType.TAB_SWITCH, evt.eventType());
        assertEquals(2, evt.severity());
        assertEquals(5, evt.riskPoints());
        assertEquals(5, evt.totalRiskScore());
    }

    @Test
    void recordEvent_tabSwitchExceedingLimit_escalatesSeverity() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        UUID eventId = UUID.randomUUID();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.TAB_SWITCH)).thenReturn(3L);
        when(eventRepository.saveAndFlush(any(SafeguardEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.TAB_SWITCH));

        // 4.º cambio de pestaña con maxTabSwitches = 3 => severidad 4, 20 puntos
        assertEquals(4, result.severity());
        assertEquals(20, result.riskPoints());
        assertEquals(20, session.getRiskScore());
    }

    @Test
    void recordEvent_maxSeverityEvent_assignsHighestPoints() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        UUID eventId = UUID.randomUUID();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.MULTIPLE_FACES)).thenReturn(0L);
        when(eventRepository.saveAndFlush(any(SafeguardEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.MULTIPLE_FACES));

        assertEquals(5, result.severity());
        assertEquals(35, result.riskPoints());
    }

    @Test
    void recordEvent_duplicateEventId_returnsExistingWithoutPersistingOrPublishing() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        session.setRiskScore(12);
        UUID eventId = UUID.randomUUID();

        SafeguardEvent existing = new SafeguardEvent();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setEventId(eventId);
        existing.setEventType(SafeguardEventType.WINDOW_BLUR);
        existing.setSeverity(1);
        existing.setRiskPoints(2);

        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.of(existing));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.WINDOW_BLUR));

        assertTrue(result.duplicate());
        assertEquals(existing.getId(), result.id());
        assertEquals(eventId, result.eventId());
        assertEquals(12, result.totalRiskScore());
        assertEquals(12, session.getRiskScore());
        verify(eventRepository, never()).saveAndFlush(any());
        verify(eventRepository, never()).countBySessionIdAndEventType(any(), any());
        verifyNoInteractions(publisher);
    }

    @Test
    void recordEvent_duplicateOnClosedSession_stillReturnsDuplicate() {
        SafeguardSession session = session(SafeguardSessionStatus.COMPLETED);
        session.setEndedAt(Instant.now());
        UUID eventId = UUID.randomUUID();
        SafeguardEvent existing = new SafeguardEvent();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setEventId(eventId);

        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.of(existing));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.TAB_SWITCH));

        assertTrue(result.duplicate());
        verifyNoInteractions(publisher);
    }

    @Test
    void recordEvent_sessionNotActive_throwsInvalidState() {
        SafeguardSession session = session(SafeguardSessionStatus.COMPLETED);
        session.setEndedAt(Instant.now());
        UUID eventId = UUID.randomUUID();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.recordEvent(eventCmd(eventId, SafeguardEventType.TAB_SWITCH)));

        assertEquals(Reason.INVALID_STATE, ex.getReason());
        verify(eventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void recordEvent_sessionOfAnotherStudent_throwsForbidden() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        session.setStudentId(UUID.randomUUID());
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.recordEvent(eventCmd(UUID.randomUUID(), SafeguardEventType.TAB_SWITCH)));

        assertEquals(Reason.FORBIDDEN, ex.getReason());
        verifyNoInteractions(eventRepository, publisher);
    }

    @Test
    void recordEvent_sessionNotFound_throwsNotFound() {
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.empty());

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.recordEvent(eventCmd(UUID.randomUUID(), SafeguardEventType.TAB_SWITCH)));

        assertEquals(Reason.NOT_FOUND, ex.getReason());
        verifyNoInteractions(eventRepository, publisher);
    }

    @Test
    void recordEvent_thresholdReached_flagsSessionAndPublishesStatusChange() {
        config.setRiskThreshold(5);
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        UUID eventId = UUID.randomUUID();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.TAB_SWITCH)).thenReturn(0L);
        when(eventRepository.saveAndFlush(any(SafeguardEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.TAB_SWITCH));

        assertEquals(SafeguardSessionStatus.FLAGGED, result.sessionStatus());
        assertNull(session.getEndedAt());

        List<Object> published = capturePublished(2);
        assertInstanceOf(SafeguardDomainEvent.RiskEventRecorded.class, published.get(0));
        SafeguardDomainEvent.SessionStatusChanged changed =
                assertInstanceOf(SafeguardDomainEvent.SessionStatusChanged.class, published.get(1));
        assertEquals(SafeguardSessionStatus.ACTIVE, changed.previousStatus());
        assertEquals(SafeguardSessionStatus.FLAGGED, changed.newStatus());
        assertEquals(5, changed.totalRiskScore());
    }

    @Test
    void recordEvent_thresholdReachedWithAutoTerminate_terminatesSession() {
        config.setRiskThreshold(5);
        config.setAutoTerminateOnThreshold(true);
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        UUID eventId = UUID.randomUUID();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.TAB_SWITCH)).thenReturn(0L);
        when(eventRepository.saveAndFlush(any(SafeguardEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordEventResult result = service.recordEvent(eventCmd(eventId, SafeguardEventType.TAB_SWITCH));

        assertEquals(SafeguardSessionStatus.TERMINATED, result.sessionStatus());
        assertNotNull(session.getEndedAt());

        List<Object> published = capturePublished(2);
        SafeguardDomainEvent.SessionStatusChanged changed =
                assertInstanceOf(SafeguardDomainEvent.SessionStatusChanged.class, published.get(1));
        assertEquals(SafeguardSessionStatus.TERMINATED, changed.newStatus());
    }

    @Test
    void recordEvent_futureOccurredAt_isClampedToServerTime() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        UUID eventId = UUID.randomUUID();
        Instant future = Instant.now().plusSeconds(3600);
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(eventRepository.findBySessionIdAndEventId(SESSION_ID, eventId)).thenReturn(Optional.empty());
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.WINDOW_BLUR)).thenReturn(0L);
        when(eventRepository.saveAndFlush(any(SafeguardEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordEvent(new RecordEventCommand(SESSION_ID, STUDENT_ID, eventId,
                SafeguardEventType.WINDOW_BLUR, future, null));

        ArgumentCaptor<SafeguardEvent> saved = ArgumentCaptor.forClass(SafeguardEvent.class);
        verify(eventRepository).saveAndFlush(saved.capture());
        assertTrue(saved.getValue().getOccurredAt().isBefore(future));
    }

    // ---------- heartbeat ----------

    @Test
    void heartbeat_activeSession_updatesLastHeartbeat() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        Instant before = session.getLastHeartbeatAt();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        HeartbeatResult result = service.heartbeat(SESSION_ID, STUDENT_ID);

        assertFalse(result.closed());
        assertEquals(SafeguardSessionStatus.ACTIVE, result.status());
        assertTrue(session.getLastHeartbeatAt().isAfter(before));
        verifyNoInteractions(publisher);
    }

    @Test
    void heartbeat_closedSession_doesNotUpdateAndReportsClosed() {
        SafeguardSession session = session(SafeguardSessionStatus.COMPLETED);
        session.setEndedAt(Instant.now().minusSeconds(5));
        Instant before = session.getLastHeartbeatAt();
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        HeartbeatResult result = service.heartbeat(SESSION_ID, STUDENT_ID);

        assertTrue(result.closed());
        assertEquals(before, session.getLastHeartbeatAt());
    }

    // ---------- closeSession ----------

    @Test
    void closeSession_activeSession_completesAndPublishesSessionClosed() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        session.setRiskScore(7);
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        SessionView view = service.closeSession(SESSION_ID, STUDENT_ID);

        assertEquals(SafeguardSessionStatus.COMPLETED, view.status());
        assertNotNull(view.endedAt());
        assertNotNull(session.getEndedAt());

        SafeguardDomainEvent.SessionClosed evt = assertInstanceOf(
                SafeguardDomainEvent.SessionClosed.class, capturePublished(1).get(0));
        assertEquals(SESSION_ID, evt.sessionId());
        assertEquals(SafeguardSessionStatus.COMPLETED, evt.finalStatus());
        assertEquals(7, evt.totalRiskScore());
    }

    @Test
    void closeSession_flaggedSession_keepsFlaggedStatus() {
        SafeguardSession session = session(SafeguardSessionStatus.FLAGGED);
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        SessionView view = service.closeSession(SESSION_ID, STUDENT_ID);

        assertEquals(SafeguardSessionStatus.FLAGGED, view.status());
        assertNotNull(session.getEndedAt());
        SafeguardDomainEvent.SessionClosed evt = assertInstanceOf(
                SafeguardDomainEvent.SessionClosed.class, capturePublished(1).get(0));
        assertEquals(SafeguardSessionStatus.FLAGGED, evt.finalStatus());
    }

    @Test
    void closeSession_alreadyClosed_isIdempotentAndDoesNotPublish() {
        SafeguardSession session = session(SafeguardSessionStatus.COMPLETED);
        Instant endedAt = Instant.now().minusSeconds(120);
        session.setEndedAt(endedAt);
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        SessionView view = service.closeSession(SESSION_ID, STUDENT_ID);

        assertEquals(endedAt, view.endedAt());
        assertEquals(SafeguardSessionStatus.COMPLETED, view.status());
        verifyNoInteractions(publisher);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void closeSession_sessionOfAnotherStudent_throwsForbidden() {
        SafeguardSession session = session(SafeguardSessionStatus.ACTIVE);
        session.setStudentId(UUID.randomUUID());
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.closeSession(SESSION_ID, STUDENT_ID));

        assertEquals(Reason.FORBIDDEN, ex.getReason());
        assertNull(session.getEndedAt());
        verifyNoInteractions(publisher);
    }

    @Test
    void closeSession_sessionNotFound_throwsNotFound() {
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.empty());

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.closeSession(SESSION_ID, STUDENT_ID));

        assertEquals(Reason.NOT_FOUND, ex.getReason());
        verifyNoInteractions(publisher);
    }

    // ---------- upsertConfiguration ----------

    private UpsertConfigCommand upsertCmd(int maxTabSwitches, int riskThreshold) {
        return new UpsertConfigCommand(EXAM_ID, true, true, false, true, true,
                maxTabSwitches, riskThreshold, true);
    }

    @Test
    void upsertConfiguration_examMissing_throwsNotFound() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(false);

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.upsertConfiguration(upsertCmd(3, 100)));

        assertEquals(Reason.NOT_FOUND, ex.getReason());
        verifyNoInteractions(configurationRepository, publisher);
    }

    @Test
    void upsertConfiguration_invalidRange_throwsIllegalArgumentWithoutTouchingRepository() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.upsertConfiguration(upsertCmd(-1, 100)));
        assertThrows(IllegalArgumentException.class,
                () -> service.upsertConfiguration(upsertCmd(3, 0)));

        verifyNoInteractions(configurationRepository, publisher);
    }

    @Test
    void upsertConfiguration_newConfig_persistsAllFieldsAndReturnsView() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(configurationRepository.findByExamId(EXAM_ID)).thenReturn(Optional.empty());
        when(configurationRepository.saveAndFlush(any(SafeguardConfiguration.class))).thenAnswer(inv -> {
            SafeguardConfiguration c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });

        ConfigView view = service.upsertConfiguration(upsertCmd(5, 80));

        assertEquals(5, view.maxTabSwitches());
        assertEquals(80, view.riskThreshold());
        assertTrue(view.autoTerminateOnThreshold());

        ArgumentCaptor<SafeguardConfiguration> saved =
                ArgumentCaptor.forClass(SafeguardConfiguration.class);
        verify(configurationRepository).saveAndFlush(saved.capture());
        assertEquals(EXAM_ID, saved.getValue().getExamId());
        assertEquals(5, saved.getValue().getMaxTabSwitches());
        assertEquals(80, saved.getValue().getRiskThreshold());
        assertTrue(saved.getValue().isRequireCamera());
        assertTrue(saved.getValue().isBlockCopyPaste());
        verifyNoInteractions(publisher);
    }

    @Test
    void upsertConfiguration_existingConfig_updatesSameInstance() {
        when(examAttemptPort.examExists(EXAM_ID)).thenReturn(true);
        when(configurationRepository.findByExamId(EXAM_ID)).thenReturn(Optional.of(config));
        when(configurationRepository.saveAndFlush(any(SafeguardConfiguration.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigView view = service.upsertConfiguration(new UpsertConfigCommand(EXAM_ID, false, false, false,
                false, false, 10, 50, false));

        assertFalse(view.enabled());
        assertFalse(view.autoTerminateOnThreshold());
        // se reutiliza la instancia existente (misma lógica que aplica el adapter sobre la entidad gestionada)
        assertFalse(config.isEnabled());
        assertEquals(10, config.getMaxTabSwitches());
        assertEquals(50, config.getRiskThreshold());
        verify(configurationRepository).saveAndFlush(same(config));
    }

    // ---------- assertStudentMayReadConfiguration ----------

    @Test
    void assertStudent_hasAttempt_doesNotThrow() {
        when(examAttemptPort.attemptExistsForStudent(EXAM_ID, STUDENT_ID)).thenReturn(true);

        service.assertStudentMayReadConfiguration(EXAM_ID, STUDENT_ID);
    }

    @Test
    void assertStudent_noAttempt_throwsForbidden() {
        when(examAttemptPort.attemptExistsForStudent(EXAM_ID, STUDENT_ID)).thenReturn(false);

        SafeguardException ex = assertThrows(SafeguardException.class,
                () -> service.assertStudentMayReadConfiguration(EXAM_ID, STUDENT_ID));

        assertEquals(Reason.FORBIDDEN, ex.getReason());
    }
}