package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 06: verifica que la migracion Sentinel se aplica con Flyway sobre
 * Postgres real y que las tablas e indices esperados existen.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SentinelSchemaMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
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

    @Autowired private JdbcTemplate jdbcTemplate;

    private List<String> columns(String table) {
        return jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = ?",
            String.class, table);
    }

    private List<String> indexes(String table) {
        return jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = ?",
            String.class, table);
    }

    @Test
    void suspiciousEventsTableHasAssociationAndAuditColumns() {
        assertThat(columns("sentinel_suspicious_events")).contains(
            "id", "event_id", "exam_id", "exam_session_id", "student_id", "attempt_id",
            "type", "source", "payload", "occurred_at", "created_at");
    }

    @Test
    void alertsTableHasAlertTypeLifecycleAndAssociationColumns() {
        assertThat(columns("sentinel_alerts")).contains(
            "id", "event_id", "alert_type", "exam_id", "exam_session_id", "student_id",
            "attempt_id", "severity", "reason", "source_event_ids", "generated_at",
            "resolved", "created_at");
    }

    @Test
    void sentinelIndexesExist() {
        assertThat(indexes("sentinel_suspicious_events")).contains(
            "idx_sentinel_events_exam",
            "idx_sentinel_events_attempt",
            "idx_sentinel_events_event_id",
            "idx_sentinel_events_source");

        assertThat(indexes("sentinel_alerts")).contains(
            "idx_sentinel_alerts_exam",
            "idx_sentinel_alerts_attempt",
            "idx_sentinel_alerts_alert_type",
            "idx_sentinel_alerts_resolved",
            "idx_sentinel_alerts_event_id");
    }
}
