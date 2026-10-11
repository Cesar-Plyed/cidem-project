package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.*;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
    "spring.test.database.replace=none",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
@Import({JpaLogEntryRepositoryAdapter.class, JpaIntegritySnapshotRepositoryAdapter.class})
class JpaOversightPersistenceAdaptersTest {

    @Container
    static GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
        .withEnv("POSTGRES_DB", "oversight_test")
        .withEnv("POSTGRES_USER", "test")
        .withEnv("POSTGRES_PASSWORD", "test")
        .withExposedPorts(5432)
        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
            "jdbc:postgresql://%s:%d/oversight_test".formatted(postgres.getHost(), postgres.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private JpaLogEntryRepositoryAdapter logEntryAdapter;

    @Autowired
    private JpaIntegritySnapshotRepositoryAdapter snapshotAdapter;

    @PersistenceContext
    private EntityManager entityManager;

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private LogEntry newLogEntry(ExamSessionId session, AttemptId attempt) {
        return new LogEntry(
            UUID.randomUUID(), session, new StudentId(UUID.randomUUID()), attempt,
            LogSource.SENTINEL, AlertSeverity.MEDIUM, "Evento de prueba", now(), now()
        );
    }

    private IntegritySnapshot newSnapshot(ExamSessionId session, AttemptId attempt) {
        return new IntegritySnapshot(
            UUID.randomUUID(), session, new StudentId(UUID.randomUUID()), attempt,
            80, RiskLevel.LOW, now()
        );
    }

    @Test
    void shouldRoundTripLogEntry() {
        ExamSessionId session = new ExamSessionId(UUID.randomUUID());
        LogEntry entry = newLogEntry(session, new AttemptId(UUID.randomUUID()));

        logEntryAdapter.save(entry);
        flushAndClear();

        assertThat(logEntryAdapter.findByExamSessionId(session)).containsExactly(entry);
    }

    @Test
    void shouldFindLogEntriesByAttemptId() {
        ExamSessionId session = new ExamSessionId(UUID.randomUUID());
        AttemptId attempt = new AttemptId(UUID.randomUUID());
        LogEntry entry = newLogEntry(session, attempt);

        logEntryAdapter.save(entry);
        flushAndClear();

        assertThat(logEntryAdapter.findByAttemptId(attempt)).containsExactly(entry);
    }

    @Test
    void shouldRoundTripIntegritySnapshot() {
        ExamSessionId session = new ExamSessionId(UUID.randomUUID());
        AttemptId attempt = new AttemptId(UUID.randomUUID());
        IntegritySnapshot snapshot = newSnapshot(session, attempt);

        snapshotAdapter.save(snapshot);
        flushAndClear();

        assertThat(snapshotAdapter.findByAttemptId(attempt)).contains(snapshot);
    }

    @Test
    void shouldReturnEmptyWhenNoSnapshotExistsForAttempt() {
        Optional<IntegritySnapshot> result = snapshotAdapter.findByAttemptId(new AttemptId(UUID.randomUUID()));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectDuplicateSnapshotForSameAttempt() {
    ExamSessionId session = new ExamSessionId(UUID.randomUUID());
    AttemptId attempt = new AttemptId(UUID.randomUUID());

    snapshotAdapter.save(newSnapshot(session, attempt));
    flushAndClear();

    assertThatThrownBy(() -> {
        snapshotAdapter.save(newSnapshot(session, attempt));
        flushAndClear();
    }).hasMessageContaining("uq_oversight_snapshot_attempt");
}

 
}