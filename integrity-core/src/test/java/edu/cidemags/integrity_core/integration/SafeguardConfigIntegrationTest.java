package edu.cidemags.integrity_core.integration;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del mapeo persistente de SafeGuard (entity + mapper + adapter)
 * contra Postgres real: la subida de configuración (PUT) es la única vía que inserta
 * filas de safeguard_configuration, y hasta ahora no existía cobertura del esquema V7.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class SafeguardConfigIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("integrity_core_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", () -> "integration-test-secret-of-at-least-32-chars!!");
        registry.add("security.jwt.access-token-expiration-minutes", () -> "15");
        registry.add("security.jwt.refresh-token-expiration-days", () -> "30");
        registry.add("security.registration-codes.administrativo", () -> "admin-test-code");
        registry.add("security.registration-codes.profesor", () -> "profesor-test-code");
        registry.add("security.rate-limit.max-attempts", () -> "1000");
        registry.add("security.rate-limit.window-seconds", () -> "600");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CountryJpaRepository countryJpaRepository;

    private UUID examId;
    private UUID studentId;

    @BeforeEach
    void seedExamAndStudent() {
        UUID countryId = countryJpaRepository.findByIsoCode("MEX")
                .orElseGet(() -> countryJpaRepository.save(new CountryEntity(null, "Mexico", "MEX")))
                .getId();

        UUID professorId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, country_id, first_names, last_names, email, password_hash, role) "
                        + "VALUES (?, ?, 'Safe', 'Guard', ?, 'x', 'PROFESOR')",
                professorId, countryId, "prof-" + UUID.randomUUID() + "@test.local");

        examId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO exams (id, title, created_by_user_id, status) VALUES (?, 'Examen SafeGuard', ?, 'DRAFT')",
                examId, professorId);

        studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, country_id, first_names, last_names, email, password_hash, role) "
                        + "VALUES (?, ?, 'Alum', 'No', ?, 'x', 'ALUMNO')",
                studentId, countryId, "alu-" + UUID.randomUUID() + "@test.local");
    }

    private static String configBody(int maxTabSwitches, int riskThreshold) {
        return "{\"enabled\":true,\"requireCamera\":true,\"requireMicrophone\":false,"
                + "\"requireFullscreen\":true,\"blockCopyPaste\":true,"
                + "\"maxTabSwitches\":" + maxTabSwitches
                + ",\"riskThreshold\":" + riskThreshold
                + ",\"autoTerminateOnThreshold\":true}";
    }

    // ---------- PUT + lectura ----------

    @Test
    void professorCreatesConfig_thenReadsItBack_thenUpdatesIt() throws Exception {
        // 1. Alta: inserta la fila (generación de id + @PrePersist + timestamps)
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody(5, 80)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examId").value(examId.toString()))
                .andExpect(jsonPath("$.maxTabSwitches").value(5))
                .andExpect(jsonPath("$.riskThreshold").value(80))
                .andExpect(jsonPath("$.requireCamera").value(true))
                .andExpect(jsonPath("$.autoTerminateOnThreshold").value(true));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT max_tab_switches, risk_threshold, enabled, version FROM safeguard_configuration "
                        + "WHERE exam_id = ?", examId);
        assertEquals(5, ((Number) row.get("max_tab_switches")).intValue());
        assertEquals(80, ((Number) row.get("risk_threshold")).intValue());
        assertEquals(0, ((Number) row.get("version")).intValue());

        // 2. Lectura por staff
        mockMvc.perform(get("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxTabSwitches").value(5))
                .andExpect(jsonPath("$.riskThreshold").value(80));

        // 3. Actualización (upsert sobre fila existente: managed entity + @Version)
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody(9, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxTabSwitches").value(9));

        Integer updated = jdbcTemplate.queryForObject(
                "SELECT max_tab_switches FROM safeguard_configuration WHERE exam_id = ?",
                Integer.class, examId);
        assertEquals(9, updated);
    }

    @Test
    void put_nonexistentExam_returns404AndInsertsNothing() throws Exception {
        UUID missingExam = UUID.randomUUID();

        mockMvc.perform(put("/api/safeguard/exams/{id}/config", missingExam)
                        .with(user("prof").roles("PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody(3, 100)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safeguard_configuration WHERE exam_id = ?", Integer.class, missingExam);
        assertEquals(0, count);
    }

    @Test
    void put_negativeMaxTabSwitches_returns400WithoutInserting() throws Exception {
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody(-1, 100)))
                .andExpect(status().isBadRequest());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safeguard_configuration WHERE exam_id = ?", Integer.class, examId);
        assertEquals(0, count);
    }

    @Test
    void get_unconfiguredExam_returns404() throws Exception {
        mockMvc.perform(get("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ---------- Lectura de alumno (regla aprobada) ----------

    @Test
    void studentReadsConfig_onlyWhenHavingAnAttempt() throws Exception {
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody(3, 100)))
                .andExpect(status().isOk());

        // Sin intento -> 403
        mockMvc.perform(get("/api/safeguard/exams/{id}/config", examId)
                        .with(user(studentId.toString()).roles("ALUMNO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Con intento -> 200 (currentStudentId() = nombre del usuario autenticado)
        jdbcTemplate.update(
                "INSERT INTO exam_attempts (id, exam_id, student_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), examId, studentId);

        mockMvc.perform(get("/api/safeguard/exams/{id}/config", examId)
                        .with(user(studentId.toString()).roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxTabSwitches").value(3));
    }
}
