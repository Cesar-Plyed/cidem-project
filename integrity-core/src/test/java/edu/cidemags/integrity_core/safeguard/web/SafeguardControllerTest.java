package edu.cidemags.integrity_core.safeguard.web;

import edu.cidemags.integrity_core.auth.infrastructure.security.JwtAuthenticationFilter;
import edu.cidemags.integrity_core.auth.infrastructure.security.SecurityConfig;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.service.SafeguardException;
import edu.cidemags.integrity_core.safeguard.service.SafeguardException.Reason;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.RecordEventResult;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.SessionView;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.StartSessionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SafeguardController.class, 
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        JwtAuthenticationFilter.class,
                        SecurityConfig.class
                }))
@AutoConfigureMockMvc(addFilters = false)
class SafeguardControllerTest {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID EVENT_DB_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SafeguardService service;
    @MockitoBean private CurrentStudentProvider studentProvider;

    @BeforeEach
    void setUp() {
        when(studentProvider.currentStudentId()).thenReturn(STUDENT_ID);
    }

    // ---------- helpers ----------

    private SessionView sessionView(SafeguardSessionStatus status, Instant endedAt) {
        Instant now = Instant.now();
        return new SessionView(SESSION_ID, EXAM_ID, ATTEMPT_ID, status, 0,
                now, endedAt, now, true, false, true, true);
    }

    private static String startBody(String examId, String attemptId) {
        StringBuilder sb = new StringBuilder("{");
        if (examId != null) sb.append("\"examId\":\"").append(examId).append("\"");
        if (attemptId != null) {
                if (examId != null) sb.append(",");
                sb.append("\"attemptId\":\"").append(attemptId).append("\"");
        }
        return sb.append("}").toString();
    }

    private static String eventBody(String eventId, String eventType) {
    StringBuilder sb = new StringBuilder("{\"sessionId\":\"").append(SESSION_ID).append("\"");
    if (eventId != null) sb.append(",\"eventId\":\"").append(eventId).append("\"");
    if (eventType != null) sb.append(",\"eventType\":\"").append(eventType).append("\"");
    return sb.append("}").toString();
}

    // ---------- POST /sessions/start ----------

    @Test
    void start_validRequest_returns200AndUsesAuthenticatedStudent() throws Exception {
        when(service.startSession(any(StartSessionCommand.class)))
                .thenReturn(sessionView(SafeguardSessionStatus.ACTIVE, null));

        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":\"" + EXAM_ID + "\",\"attemptId\":\"" + ATTEMPT_ID + "\",\"userAgent\":\"JUnit-UA\"}"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()))
                .andExpect(jsonPath("$.attemptId").value(ATTEMPT_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.requireCamera").value(true))
                .andExpect(jsonPath("$.requireMicrophone").value(false));

        ArgumentCaptor<StartSessionCommand> captor = ArgumentCaptor.forClass(StartSessionCommand.class);
        verify(service).startSession(captor.capture());
        StartSessionCommand cmd = captor.getValue();
        assertEquals(EXAM_ID, cmd.examId());
        assertEquals(ATTEMPT_ID, cmd.attemptId());
        assertEquals(STUDENT_ID, cmd.studentId());
        assertEquals("JUnit-UA", cmd.userAgent());
        assertEquals("127.0.0.1", cmd.clientIp());
    }

    @Test
    void start_missingExamId_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody(null, ATTEMPT_ID.toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.examId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void start_missingAttemptId_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody(EXAM_ID.toString(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.attemptId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void start_emptyBody_returns400WithAllFieldErrors() throws Exception {
        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.examId").exists())
                .andExpect(jsonPath("$.errors.attemptId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void start_invalidUuids_returns400() throws Exception {
        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody("no-uuid", "tampoco")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void start_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void start_examNotFound_returns404() throws Exception {
        when(service.startSession(any(StartSessionCommand.class)))
                .thenThrow(new SafeguardException(Reason.NOT_FOUND, "Exam not found: 1"));

        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody(EXAM_ID.toString(), ATTEMPT_ID.toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void start_attemptOfAnotherStudent_returns403() throws Exception {
        when(service.startSession(any(StartSessionCommand.class)))
                .thenThrow(new SafeguardException(Reason.FORBIDDEN, "Attempt does not belong to student/exam"));

        mockMvc.perform(post("/api/safeguard/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody(EXAM_ID.toString(), ATTEMPT_ID.toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ---------- POST /events ----------

    @Test
    void recordEvent_newEvent_returns201WithComputedSeverity() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(service.recordEvent(any(RecordEventCommand.class))).thenReturn(
               new RecordEventResult(EVENT_DB_ID, eventId, 2, 5, 5, SafeguardSessionStatus.ACTIVE, false));

        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(eventId.toString(), "TAB_SWITCH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(EVENT_DB_ID.toString()))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.severity").value(2))
                .andExpect(jsonPath("$.riskPoints").value(5))
                .andExpect(jsonPath("$.totalRiskScore").value(5))
                .andExpect(jsonPath("$.sessionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.duplicate").value(false));

        ArgumentCaptor<RecordEventCommand> captor = ArgumentCaptor.forClass(RecordEventCommand.class);
        verify(service).recordEvent(captor.capture());
        RecordEventCommand cmd = captor.getValue();
        assertEquals(SESSION_ID, cmd.sessionId());
        assertEquals(STUDENT_ID, cmd.studentId());
        assertEquals(eventId, cmd.eventId());
        assertEquals(SafeguardEventType.TAB_SWITCH, cmd.eventType());
    }

    @Test
    void recordEvent_duplicate_returns200() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(service.recordEvent(any(RecordEventCommand.class))).thenReturn(
               new RecordEventResult(EVENT_DB_ID, eventId, 2, 5, 5, SafeguardSessionStatus.ACTIVE, true));

        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(eventId.toString(), "TAB_SWITCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void recordEvent_missingEventId_returns400() throws Exception {
        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(null, "TAB_SWITCH")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void recordEvent_missingEventType_returns400() throws Exception {
        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(UUID.randomUUID().toString(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventType").exists());

        verifyNoInteractions(service);
    }

    @Test
    void recordEvent_unknownEventType_returns400() throws Exception {
        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(UUID.randomUUID().toString(), "NOT_A_TYPE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void recordEvent_invalidUuid_returns400() throws Exception {
        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("not-a-uuid", "TAB_SWITCH")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void recordEvent_sessionNotActive_returns409() throws Exception {
        when(service.recordEvent(any(RecordEventCommand.class)))
                .thenThrow(new SafeguardException(Reason.INVALID_STATE, "Session is not active"));

        mockMvc.perform(post("/api/safeguard/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(UUID.randomUUID().toString(), "TAB_SWITCH")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    // ---------- POST /sessions/{id}/close ----------

    @Test
    void close_activeSession_returns200WithCompletedStatus() throws Exception {
        when(service.closeSession(SESSION_ID, STUDENT_ID))
                .thenReturn(sessionView(SafeguardSessionStatus.COMPLETED, Instant.now()));

        mockMvc.perform(post("/api/safeguard/sessions/{id}/close", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endedAt").exists());

        verify(service).closeSession(SESSION_ID, STUDENT_ID);
    }

    @Test
    void close_sessionNotFound_returns404() throws Exception {
        when(service.closeSession(any(UUID.class), any(UUID.class)))
                .thenThrow(new SafeguardException(Reason.NOT_FOUND, "Session not found: 10"));

        mockMvc.perform(post("/api/safeguard/sessions/{id}/close", SESSION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void close_sessionOfAnotherStudent_returns403() throws Exception {
        when(service.closeSession(any(UUID.class), any(UUID.class)))
                .thenThrow(new SafeguardException(Reason.FORBIDDEN, "Session does not belong to student"));

        mockMvc.perform(post("/api/safeguard/sessions/{id}/close", SESSION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}