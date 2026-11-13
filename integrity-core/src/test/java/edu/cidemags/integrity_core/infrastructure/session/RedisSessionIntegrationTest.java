package edu.cidemags.integrity_core.infrastructure.session;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración: persistencia de {@link RedisSessionEntity} contra Postgres real
 * (Testcontainers). Verifica INSERT, SELECT, UPDATE de estado, y que la tabla existe.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class RedisSessionIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("integrity_core_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void insertSession_persistsAndReadsBack() {
        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO exam_session (id, exam_id, attempt_id, student_id, status, started_at, max_duration_seconds) "
                        + "VALUES (?, ?, ?, ?, 'RUNNING', ?, 1800)",
                sessionId, examId, attemptId, studentId, Instant.now());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getInt(1), sessionId);
        assertThat(count).isEqualTo(1);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT exam_id, attempt_id, student_id, status, started_at, max_duration_seconds FROM exam_session WHERE id = ?", sessionId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("exam_id")).isEqualTo(examId.toString());
        assertThat(rows.get(0).get("attempt_id")).isEqualTo(attemptId.toString());
        assertThat(rows.get(0).get("student_id")).isEqualTo(studentId.toString());
        assertThat(rows.get(0).get("status")).isEqualTo("RUNNING");
    }

    @Test
    void updateSessionStatus_changesRow() {
        UUID sessionId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO exam_session (id, exam_id, attempt_id, student_id, status, started_at, max_duration_seconds) "
                        + "VALUES (?, ?, ?, ?, 'RUNNING', ?, 1800)",
                sessionId, examId, attemptId, studentId, Instant.now());

        jdbcTemplate.update(
                "UPDATE exam_session SET status = 'COMPLETED', finished_at = ? WHERE id = ?",
                Instant.now(), sessionId);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getString(1), sessionId);
        assertThat(status).isEqualTo("COMPLETED");
    }

    @Test
    void deleteSession_removesRow() {
        UUID sessionId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO exam_session (id, exam_id, attempt_id, student_id, status, started_at, max_duration_seconds) "
                        + "VALUES (?, ?, ?, ?, 'RUNNING', ?, 1800)",
                sessionId, examId, attemptId, studentId, Instant.now());

        jdbcTemplate.update("DELETE FROM exam_session WHERE id = ?", sessionId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getInt(1), sessionId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void sessionWithPayload_persistsCorrectly() {
        UUID sessionId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        String payload = "{\"question\":\"¿Cuál es la capital de Francia?\",\"answer\":\"Paris\",\"score\":1}";

        jdbcTemplate.update(
                "INSERT INTO exam_session (id, exam_id, attempt_id, student_id, status, started_at, max_duration_seconds, payload) "
                        + "VALUES (?, ?, ?, ?, 'RUNNING', ?, 1800, ?)",
                sessionId, examId, attemptId, studentId, Instant.now(), payload);

        String readPayload = jdbcTemplate.queryForObject(
                "SELECT payload FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getString(1), sessionId);
        assertThat(readPayload).isEqualTo(payload);
    }
}
