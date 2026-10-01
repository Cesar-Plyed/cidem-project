package edu.cidemags.integrity_core.safeguard.infrastructure.web.controller;

import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.infrastructure.security.SecurityConfig;
import edu.cidemags.integrity_core.safeguard.application.dto.ConfigView;
import edu.cidemags.integrity_core.safeguard.application.dto.UpsertConfigCommand;
import edu.cidemags.integrity_core.safeguard.application.usecase.SafeguardService;
import edu.cidemags.integrity_core.safeguard.domain.exception.SafeguardException;
import edu.cidemags.integrity_core.safeguard.domain.exception.SafeguardException.Reason;
import edu.cidemags.integrity_core.safeguard.infrastructure.security.CurrentStudentProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regla aprobada (2026-10-01): escritura de configuración SOLO PROFESOR/ADMINISTRATIVO;
 * ALUMNO puede leer solo si tiene un intento en el examen.
 * Mismo patrón que AdminRegisterAuthorizationTest: SecurityConfig aporta @EnableMethodSecurity.
 */
@WebMvcTest(controllers = SafeguardController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class SafeguardConfigAuthorizationTest {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SafeguardService service;
    @MockitoBean private CurrentStudentProvider studentProvider;
    // Dependencia de JwtAuthenticationFilter dentro del slice @WebMvcTest
    @MockitoBean private TokenProvider tokenProvider;

    private static String body() {
        return "{\"enabled\":true,\"requireCamera\":true,\"requireMicrophone\":false,"
                + "\"requireFullscreen\":true,\"blockCopyPaste\":true,"
                + "\"maxTabSwitches\":3,\"riskThreshold\":100,\"autoTerminateOnThreshold\":false}";
    }

    private void stubStaffGet() {
        when(service.getConfiguration(EXAM_ID))
                .thenReturn(new ConfigView(EXAM_ID, true, true, false, true, true, 3, 100, false));
    }

    // ---------- PUT escritura ----------

    @Test
    @WithMockUser(roles = "PROFESOR")
    void put_asProfesor_returns200() throws Exception {
        when(service.upsertConfiguration(any(UpsertConfigCommand.class)))
                .thenReturn(new ConfigView(EXAM_ID, true, true, false, true, true, 3, 100, false));

        mockMvc.perform(put("/api/safeguard/exams/{id}/config", EXAM_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxTabSwitches").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void put_asAdministrativo_returns200() throws Exception {
        when(service.upsertConfiguration(any(UpsertConfigCommand.class)))
                .thenReturn(new ConfigView(EXAM_ID, true, true, false, true, true, 3, 100, false));

        mockMvc.perform(put("/api/safeguard/exams/{id}/config", EXAM_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void put_asAlumno_returns403WithoutTouchingService() throws Exception {
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", EXAM_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service, studentProvider);
    }

    @Test
    void put_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", EXAM_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(service, studentProvider);
    }

    // ---------- GET lectura ----------

    @Test
    @WithMockUser(roles = "PROFESOR")
    void get_asProfesor_returns200WithoutAttemptCheck() throws Exception {
        stubStaffGet();

        mockMvc.perform(get("/api/safeguard/exams/{id}/config", EXAM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskThreshold").value(100));

        verify(service, never()).assertStudentMayReadConfiguration(any(), any());
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void get_asAlumnoWithAttempt_returns200() throws Exception {
        when(studentProvider.currentStudentId()).thenReturn(STUDENT_ID);
        when(service.getConfiguration(EXAM_ID))
                .thenReturn(new ConfigView(EXAM_ID, true, true, false, true, true, 3, 100, false));

        mockMvc.perform(get("/api/safeguard/exams/{id}/config", EXAM_ID))
                .andExpect(status().isOk());

        verify(service).assertStudentMayReadConfiguration(EXAM_ID, STUDENT_ID);
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void get_asAlumnoWithoutAttempt_returns403() throws Exception {
        when(studentProvider.currentStudentId()).thenReturn(STUDENT_ID);
        doThrow(new SafeguardException(Reason.FORBIDDEN, "Student has no attempt for exam"))
                .when(service).assertStudentMayReadConfiguration(EXAM_ID, STUDENT_ID);

        mockMvc.perform(get("/api/safeguard/exams/{id}/config", EXAM_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(service, never()).getConfiguration(any());
    }

    @Test
    void get_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/safeguard/exams/{id}/config", EXAM_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service, studentProvider);
    }
}
