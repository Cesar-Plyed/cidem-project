package edu.cidemags.integrity_core.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventResult;
import edu.cidemags.integrity_core.safeguard.application.dto.StartSessionCommand;
import edu.cidemags.integrity_core.safeguard.application.usecase.SafeguardService;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida completo de una sesión de SafeGuard contra Postgres real: configuración,
 * alta, eventos que cruzan el umbral de riesgo, heartbeat, cierre e idempotencia — el
 * esquema V7 (safeguard_session/safeguard_event) nunca se había ejercitado de punta a punta.
 *
 * <p>La clase es <b>no transaccional</b> a propósito: cada llamada al servicio o a MockMvc
 * necesita su propia transacción comprometida (los asserts de BD leen lo que cada petición
 * dejó escrito). El aislamiento entre tests se logra con UUIDs frescos por test.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SafeguardSessionIntegrationTest {

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
    @Autowired private SafeguardService service;

    private UUID examId;
    private UUID attemptId;
    private UUID studentId;

    @BeforeEach
    void seedExamAttemptAndStudent() {
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

        attemptId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO exam_attempts (id, exam_id, student_id) VALUES (?, ?, ?)",
                attemptId, examId, studentId);
    }

    // ---------- Helpers ----------

    private static String configBody(int maxTabSwitches, int riskThreshold, boolean autoTerminate) {
        return "{\"enabled\":true,\"requireCamera\":true,\"requireMicrophone\":false,"
                + "\"requireFullscreen\":true,\"blockCopyPaste\":true,"
                + "\"maxTabSwitches\":" + maxTabSwitches
                + ",\"riskThreshold\":" + riskThreshold
                + ",\"autoTerminateOnThreshold\":" + autoTerminate + "}";
    }

    private void putConfig(int maxTabSwitches, int riskThreshold, boolean autoTerminate) throws Exception {
        mockMvc.perform(put("/api/safeguard/exams/{id}/config", examId)
                        .with(user("prof").roles("PROFESOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody(maxTabSwitches, riskThreshold, autoTerminate)))
                .andExpect(status().isOk());
    }

    private UUID startSessionViaHttp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/safeguard/sessions/start")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":\"" + examId + "\",\"attemptId\":\"" + attemptId
                                + "\",\"userAgent\":\"JUnit\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.sessionId"));
    }

    private static String eventBody(UUID sessionId, UUID eventId, String eventType) {
        return eventBody(sessionId, eventId, eventType, null);
    }

    /**
     * metadata opcional: contenido JSON (p. ej. {@code {"faces":3}}) que se escapa como
     * cadena JSON en el body — el DTO expone metadata como String y Jackson no acepta
     * objetos crudos para un campo String.
     */
    private static String eventBody(UUID sessionId, UUID eventId, String eventType, String metadataJson) {
        String body = "{\"sessionId\":\"" + sessionId + "\",\"eventId\":\"" + eventId
                + "\",\"eventType\":\"" + eventType + "\"";
        if (metadataJson != null) {
            body += ",\"metadata\":\"" + metadataJson.replace("\"", "\\\"") + "\"";
        }
        return body + "}";
    }

    private void recordEvent(UUID sessionId, UUID eventId, String eventType) throws Exception {
        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, eventId, eventType)))
                .andExpect(status().isCreated());
    }

    private int eventRows(UUID sessionId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safeguard_event WHERE session_id = ?", Integer.class, sessionId);
    }

    private int riskScore(UUID sessionId) {
        return jdbcTemplate.queryForObject(
                "SELECT risk_score FROM safeguard_session WHERE id = ?", Integer.class, sessionId);
    }

    // ---------- Ciclo de vida completo ----------

    @Test
    void fullLifecycle_integrityIndexBelow50_terminatesSession() throws Exception {
        // El riesgo SafeGuard llega a 55 sin auto-terminación, pero el índice de
        // integridad de oversight cruza 50 y cierra el intento en el tercer evento.
        putConfig(3, 25, false);
        UUID sessionId = startSessionViaHttp();

        recordEvent(sessionId, UUID.randomUUID(), "FULLSCREEN_EXIT"); // severidad 3 -> 10
        recordEvent(sessionId, UUID.randomUUID(), "COPY_PASTE");      // severidad 3 -> 10

        // metadata JSON sobre la columna JSONB
        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, UUID.randomUUID(), "MULTIPLE_FACES",
                                "{\"faces\":3}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRiskScore").value(55))
                .andExpect(jsonPath("$.sessionStatus").value("TERMINATED"))
                .andExpect(jsonPath("$.duplicate").value(false));

        // El heartbeat refleja el cierre disparado por el índice de integridad.
        mockMvc.perform(post("/api/safeguard/sessions/{id}/heartbeat", sessionId)
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientTime\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.riskScore").value(55))
                .andExpect(jsonPath("$.closed").value(true));

        // El cierre idempotente conserva el estado TERMINATED.
        mockMvc.perform(post("/api/safeguard/sessions/{id}/close", sessionId)
                        .with(user(studentId.toString()).roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.endedAt").exists());

        // Un heartbeat posterior informa closed sin alterar el estado.
        mockMvc.perform(post("/api/safeguard/sessions/{id}/heartbeat", sessionId)
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientTime\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.status").value("TERMINATED"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, risk_score, ended_at, version FROM safeguard_session WHERE id = ?",
                sessionId);
        assertEquals("TERMINATED", row.get("status"));
        assertEquals(55, ((Number) row.get("risk_score")).intValue());
        assertNotNull(row.get("ended_at"));
        assertTrue(((Number) row.get("version")).intValue() > 0, "cada transacción bumpa @Version");
        assertEquals(3, eventRows(sessionId));
    }

    @Test
    void thresholdWithAutoTerminate_terminatesSessionAndRejectsNewEvents() throws Exception {
        putConfig(3, 10, true); // umbral 10 exactos + auto-terminate
        UUID sessionId = startSessionViaHttp();

        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, UUID.randomUUID(), "FULLSCREEN_EXIT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRiskScore").value(10))
                .andExpect(jsonPath("$.sessionStatus").value("TERMINATED"));

        // Sesión terminada (ended_at seteado) => no acepta más eventos
        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, UUID.randomUUID(), "COPY_PASTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));

        // Cierre idempotente: ya terminada, devuelve TERMINATED sin cambios
        mockMvc.perform(post("/api/safeguard/sessions/{id}/close", sessionId)
                        .with(user(studentId.toString()).roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, ended_at FROM safeguard_session WHERE id = ?", sessionId);
        assertEquals("TERMINATED", row.get("status"));
        assertNotNull(row.get("ended_at"));
        assertEquals(1, eventRows(sessionId));
    }

    // ---------- Idempotencia ----------

    @Test
    void startSession_calledTwice_returnsSameSessionAndKeepsSingleRow() throws Exception {
        putConfig(3, 100, false);

        UUID first = startSessionViaHttp();
        UUID second = startSessionViaHttp();

        assertEquals(first, second, "reiniciar el mismo intento devuelve la misma sesión");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safeguard_session WHERE attempt_id = ?", Integer.class, attemptId);
        assertEquals(1, rows);
    }

    @Test
    void recordEvent_duplicateEventId_countsOnceAndKeepsScore() throws Exception {
        putConfig(3, 100, false);
        UUID sessionId = startSessionViaHttp();
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, eventId, "FULLSCREEN_EXIT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.totalRiskScore").value(10));

        // Mismo eventId => 200 + duplicate:true, sin sumar puntos de nuevo
        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, eventId, "FULLSCREEN_EXIT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.totalRiskScore").value(10));

        assertEquals(1, eventRows(sessionId));
        assertEquals(10, riskScore(sessionId));
    }

    // ---------- Autorización y estados ----------

    @Test
    void recordEvent_foreignStudent_returns403WithoutPersisting() throws Exception {
        putConfig(3, 100, false);
        UUID sessionId = startSessionViaHttp();

        UUID countryId = countryJpaRepository.findByIsoCode("MEX")
                .orElseGet(() -> countryJpaRepository.save(new CountryEntity(null, "Mexico", "MEX")))
                .getId();
        UUID otherStudent = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, country_id, first_names, last_names, email, password_hash, role) "
                        + "VALUES (?, ?, 'Otro', 'Alumno', ?, 'x', 'ALUMNO')",
                otherStudent, countryId, "other-" + UUID.randomUUID() + "@test.local");

        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(otherStudent.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, UUID.randomUUID(), "COPY_PASTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertEquals(0, eventRows(sessionId));
    }

    @Test
    void recordEvent_unknownSession_returns404() throws Exception {
        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(UUID.randomUUID(), UUID.randomUUID(), "TAB_SWITCH")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void recordEvent_afterClose_returns409WithoutPersisting() throws Exception {
        putConfig(3, 100, false);
        UUID sessionId = startSessionViaHttp();
        recordEvent(sessionId, UUID.randomUUID(), "WINDOW_BLUR");

        mockMvc.perform(post("/api/safeguard/sessions/{id}/close", sessionId)
                        .with(user(studentId.toString()).roles("ALUMNO")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/safeguard/events")
                        .with(user(studentId.toString()).roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(sessionId, UUID.randomUUID(), "WINDOW_BLUR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));

        assertEquals(1, eventRows(sessionId));
        assertEquals(2, riskScore(sessionId)); // solo el evento previo al cierre
    }

    // ---------- Concurrencia ----------
    // Diseño: recordEvent toma el lock pesimista de la fila de sesión (PESSIMISTIC_WRITE)
    // ANTES de la verificación de duplicado, así que las carreras se serializan sobre esa
    // única fila: no hay deadlock posible ni pérdida de incrementos. Umbral altísimo para
    // que ningún resultado cruce el límite durante la carrera.

    @Tag("concurrency")
    @Test
    void recordEvent_parallelDistinctEventIds_persistsAllAndSumsScoreExactly() throws Exception {
        putConfig(3, 100_000, false);
        UUID sessionId = service.startSession(
                new StartSessionCommand(examId, attemptId, studentId, "127.0.0.1", "JUnit")).sessionId();

        int threads = 2;
        int eventsPerThread = 2;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<List<RecordEventResult>>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                List<UUID> eventIds = new ArrayList<>();
                for (int i = 0; i < eventsPerThread; i++) {
                    eventIds.add(UUID.randomUUID());
                }
                futures.add(pool.submit(() -> {
                    start.await();
                    List<RecordEventResult> results = new ArrayList<>();
                    for (UUID eventId : eventIds) {
                        results.add(service.recordEvent(new RecordEventCommand(
                                sessionId, studentId, eventId,
                                SafeguardEventType.FULLSCREEN_EXIT, Instant.now(), null)));
                    }
                    return results;
                }));
            }
            start.countDown();

            List<RecordEventResult> all = new ArrayList<>();
            for (Future<List<RecordEventResult>> future : futures) {
                all.addAll(future.get(30, TimeUnit.SECONDS));
            }

            assertEquals(threads * eventsPerThread, all.size());
            assertTrue(all.stream().noneMatch(RecordEventResult::duplicate),
                    "eventIds distintos nunca deben marcarse duplicados");
        } finally {
            pool.shutdownNow();
        }

        // Sin pérdida de incrementos: 4 eventos x 10 puntos = 40 exactos
        assertEquals(threads * eventsPerThread, eventRows(sessionId));
        assertEquals(threads * eventsPerThread * 10, riskScore(sessionId));
    }

    @Tag("concurrency")
    @Test
    void recordEvent_parallelSameEventId_persistsOneRowAndMarksOneDuplicate() throws Exception {
        putConfig(3, 100_000, false);
        UUID sessionId = service.startSession(
                new StartSessionCommand(examId, attemptId, studentId, "127.0.0.1", "JUnit")).sessionId();
        UUID eventId = UUID.randomUUID();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<RecordEventResult>> futures = new ArrayList<>();
            for (int t = 0; t < 2; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return service.recordEvent(new RecordEventCommand(
                            sessionId, studentId, eventId,
                            SafeguardEventType.FULLSCREEN_EXIT, Instant.now(), null));
                }));
            }
            start.countDown();

            List<RecordEventResult> results = new ArrayList<>();
            for (Future<RecordEventResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            assertEquals(1, results.stream().filter(r -> !r.duplicate()).count(),
                    "exactamente un hilo debe crear el evento");
            assertEquals(1, results.stream().filter(RecordEventResult::duplicate).count(),
                    "exactamente un hilo debe tomar la ruta idempotente");
        } finally {
            pool.shutdownNow();
        }

        // Una sola fila (única (session_id, event_id)) y el puntaje solo cuenta una vez
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM safeguard_event WHERE session_id = ? AND event_id = ?",
                Integer.class, sessionId, eventId);
        assertEquals(1, rows);
        assertEquals(10, riskScore(sessionId));
    }
}
