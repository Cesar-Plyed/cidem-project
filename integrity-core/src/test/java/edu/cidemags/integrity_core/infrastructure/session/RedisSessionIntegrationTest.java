package edu.cidemags.integrity_core.infrastructure.session;

import org.junit.jupiter.api.BeforeEach;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración: persistencia de {@link RedisSessionEntity} contra Postgres real
 * (Testcontainers). Verifica INSERT, SELECT, UPDATE de estado, y que la tabla existe.
 *
 * <p>La tabla {@code exam_session} tiene FKs a exams/exam_attempts/users, así que
 * cada test siembra primero esas filas.</p>
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

    private UUID examId;
    private UUID attemptId;
    private UUID studentId;

    @BeforeEach
    void seedParents() {
        UUID countryId = jdbcTemplate.queryForObject(
                "SELECT id FROM countries WHERE iso_code = 'MEX'", UUID.class);
        if (countryId == null) {
            countryId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO countries (id, name, iso_code) VALUES (?, 'Mexico', 'MEX')",
                    countryId);
        }

        UUID professorId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, country_id, first_names, last_names, email, password_hash, role) "
                        + "VALUES (?, ?, 'Ses', 'Ion', ?, 'x', 'PROFESOR')",
                professorId, countryId, "ses-" + UUID.randomUUID() + "@test.local");

        studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, country_id, first_names, last_names, email, password_hash, role) "
                        + "VALUES (?, ?, 'Alum', 'No', ?, 'x', 'ALUMNO')",
                studentId, countryId, "ses-alu-" + UUID.randomUUID() + "@test.local");

        examId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO exams (id, title, created_by_user_id, status) VALUES (?, 'Examen sesión', ?, 'DRAFT')",
                examId, professorId);

        attemptId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO exam_attempts (id, exam_id, student_id) VALUES (?, ?, ?)",
                attemptId, examId, studentId);
    }

    private void insertSession(UUID sessionId, String payload) {
        jdbcTemplate.update(
                "INSERT INTO exam_session (id, exam_id, attempt_id, student_id, status, started_at, max_duration_seconds, payload) "
                        + "VALUES (?, ?, ?, ?, 'RUNNING', ?, 1800, ?)",
                sessionId, examId, attemptId, studentId, Timestamp.from(Instant.now()), payload);
    }

    @Test
    void insertSession_persistsAndReadsBack() {
        UUID sessionId = UUID.randomUUID();
        insertSession(sessionId, null);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getInt(1), sessionId);
        assertThat(count).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT exam_id, attempt_id, student_id, status, started_at, max_duration_seconds "
                        + "FROM exam_session WHERE id = ?",
                sessionId);
        assertThat(row.get("exam_id")).isEqualTo(examId);
        assertThat(row.get("attempt_id")).isEqualTo(attemptId);
        assertThat(row.get("student_id")).isEqualTo(studentId);
        assertThat(row.get("status")).isEqualTo("RUNNING");
        assertThat(row.get("started_at")).isNotNull();
        assertThat(((Number) row.get("max_duration_seconds")).longValue()).isEqualTo(1800L);
    }

    @Test
    void updateSessionStatus_changesRow() {
        UUID sessionId = UUID.randomUUID();
        insertSession(sessionId, null);

        jdbcTemplate.update(
                "UPDATE exam_session SET status = 'COMPLETED', finished_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), sessionId);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getString(1), sessionId);
        assertThat(status).isEqualTo("COMPLETED");
    }

    @Test
    void deleteSession_removesRow() {
        UUID sessionId = UUID.randomUUID();
        insertSession(sessionId, null);

        jdbcTemplate.update("DELETE FROM exam_session WHERE id = ?", sessionId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getInt(1), sessionId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void sessionWithPayload_persistsCorrectly() {
        UUID sessionId = UUID.randomUUID();
        String payload = "{\"question\":\"¿Cuál es la capital de Francia?\",\"answer\":\"Paris\",\"score\":1}";
        insertSession(sessionId, payload);

        String readPayload = jdbcTemplate.queryForObject(
                "SELECT payload FROM exam_session WHERE id = ?",
                (rs, rowNum) -> rs.getString(1), sessionId);
        assertThat(readPayload).isEqualTo(payload);
    }
}
