package edu.cidemags.integrity_core.sentinel.application;

import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.infrastructure.security.SecurityConfig;
import edu.cidemags.integrity_core.sentinel.application.service.AuthorizationService;
import edu.cidemags.integrity_core.sentinel.application.service.SentinelQueryService;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 10: rol por `@PreAuthorize` y propiedad del recurso por
 * {@link AuthorizationService}.
 */
@WebMvcTest(controllers = SentinelController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class SentinelControllerAuthorizationTest {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String STUDENT = "11111111-1111-1111-1111-111111111111";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReceiveSuspiciousEventUseCase receiveEventUseCase;
    @MockitoBean private SentinelQueryService queryService;
    @MockitoBean private AuthorizationService authorization;
    // Dependencia de JwtAuthenticationFilter en el slice @WebMvcTest.
    @MockitoBean private TokenProvider tokenProvider;

    private static String eventBody() {
        return """
            {"examId":"%s","attemptId":"%s","examSessionId":"%s",
             "eventType":"TAB_OR_APP_SWITCH","source":"SENTINEL","payload":"{}"}
            """.formatted(EXAM_ID, ATTEMPT_ID, SESSION_ID);
    }

    // ---------- GET /exams/{examId}/alerts ----------

    @Test
    @WithMockUser(roles = "PROFESOR")
    void getExamAlerts_asProfesor_checksExamOwnershipAndReturns200() throws Exception {
        when(queryService.findAlertsByExam(any(), any(), any(), any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/sentinel/exams/{examId}/alerts", EXAM_ID))
            .andExpect(status().isOk());

        verify(authorization).assertCanViewExamAlerts(EXAM_ID);
    }

    @Test
    @WithMockUser(roles = "PROFESOR")
    void getExamAlerts_asProfesorWithoutOwnership_returns403() throws Exception {
        doThrow(new AccessDeniedException("Sin permiso"))
            .when(authorization).assertCanViewExamAlerts(EXAM_ID);

        mockMvc.perform(get("/sentinel/exams/{examId}/alerts", EXAM_ID))
            .andExpect(status().isForbidden());

        verifyNoInteractions(queryService);
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void getExamAlerts_asAlumno_returns403() throws Exception {
        mockMvc.perform(get("/sentinel/exams/{examId}/alerts", EXAM_ID))
            .andExpect(status().isForbidden());

        verifyNoInteractions(queryService, authorization);
    }

    @Test
    void getExamAlerts_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/sentinel/exams/{examId}/alerts", EXAM_ID))
            .andExpect(status().is4xxClientError());
    }

    // ---------- GET /attempts/{attemptId}/events ----------

    @Test
    @WithMockUser(username = STUDENT, roles = "ALUMNO")
    void getAttemptEvents_asAlumno_checksOwnership() throws Exception {
        when(queryService.findEventsByAttempt(any(), anyInt()))
            .thenReturn(java.util.List.of());

        mockMvc.perform(get("/sentinel/attempts/{attemptId}/events", ATTEMPT_ID))
            .andExpect(status().isOk());

        verify(authorization).assertCanViewAttemptEvents(ATTEMPT_ID);
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "ALUMNO")
    void getAttemptEvents_asAlumnoFromAnotherAttempt_returns403() throws Exception {
        doThrow(new AccessDeniedException("Sin permiso"))
            .when(authorization).assertCanViewAttemptEvents(ATTEMPT_ID);

        mockMvc.perform(get("/sentinel/attempts/{attemptId}/events", ATTEMPT_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void getAttemptEvents_asAdministrativo_returns200() throws Exception {
        when(queryService.findEventsByAttempt(any(), anyInt()))
            .thenReturn(java.util.List.of());

        mockMvc.perform(get("/sentinel/attempts/{attemptId}/events", ATTEMPT_ID))
            .andExpect(status().isOk());
    }

    // ---------- GET /sessions/{sessionId}/alerts ----------

    @Test
    @WithMockUser(roles = "PROFESOR")
    void getSessionAlerts_asProfesor_checksSessionAccess() throws Exception {
        when(queryService.findAlertsBySession(any(), any(), any(), any()))
            .thenReturn(java.util.List.of());

        mockMvc.perform(get("/sentinel/sessions/{sessionId}/alerts", SESSION_ID))
            .andExpect(status().isOk());

        verify(authorization).assertCanViewSessionAlerts(SESSION_ID);
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void getSessionAlerts_asAlumno_returns403() throws Exception {
        mockMvc.perform(get("/sentinel/sessions/{sessionId}/alerts", SESSION_ID))
            .andExpect(status().isForbidden());

        verifyNoInteractions(queryService, authorization);
    }

    // ---------- POST /events ----------

    @Test
    @WithMockUser(username = STUDENT, roles = "ALUMNO")
    void postEvent_asAlumno_usesJwtStudentAndCreatesEvent() throws Exception {
        when(authorization.isAlumno()).thenReturn(true);
        when(authorization.currentUserId()).thenReturn(STUDENT_ID);

        mockMvc.perform(post("/sentinel/events")
                .contentType(MediaType.APPLICATION_JSON).content(eventBody()))
            .andExpect(status().isCreated());

        verify(authorization).assertCanViewAttemptEvents(ATTEMPT_ID);
        verify(receiveEventUseCase).receive(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void postEvent_asAdministrativo_isAllowed() throws Exception {
        when(authorization.currentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/sentinel/events")
                .contentType(MediaType.APPLICATION_JSON).content(eventBody()))
            .andExpect(status().isCreated());
    }

    @Test
    void postEvent_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/sentinel/events")
                .contentType(MediaType.APPLICATION_JSON).content(eventBody()))
            .andExpect(status().is4xxClientError());

        verifyNoInteractions(receiveEventUseCase);
    }

    // ---------- PUT /alerts/{alertId}/resolve ----------

    @Test
    @WithMockUser(roles = "PROFESOR")
    void resolveAlert_asProfesor_checksExamOwnership() throws Exception {
        UUID alertId = UUID.randomUUID();
        when(queryService.findAlertById(alertId)).thenReturn(Optional.of(
            new edu.cidemags.integrity_core.sentinel.application.dto.AlertView(
                alertId, EXAM_ID, ATTEMPT_ID, SESSION_ID, STUDENT_ID, "Alumno", "Examen",
                "TAB_SWITCH", edu.cidemags.integrity_core.sentinel.domain.AlertSeverity.HIGH,
                "motivo", java.time.Instant.now(), false)));

        mockMvc.perform(put("/sentinel/alerts/{alertId}/resolve", alertId))
            .andExpect(status().isNoContent());

        verify(authorization).assertCanViewExamAlerts(EXAM_ID);
        verify(queryService).resolveAlert(alertId);
    }

    @Test
    @WithMockUser(roles = "PROFESOR")
    void resolveUnknownAlert_returns404() throws Exception {
        UUID alertId = UUID.randomUUID();
        when(queryService.findAlertById(alertId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/sentinel/alerts/{alertId}/resolve", alertId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void resolveAlert_asAlumno_returns403() throws Exception {
        mockMvc.perform(put("/sentinel/alerts/{alertId}/resolve", UUID.randomUUID()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(queryService, authorization);
    }
}
