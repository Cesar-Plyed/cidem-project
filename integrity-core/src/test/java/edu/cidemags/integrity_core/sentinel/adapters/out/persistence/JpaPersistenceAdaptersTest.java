package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.test.database.replace=none",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
@Import({JpaSuspiciousEventRepositoryAdapter.class, JpaAlertRepositoryAdapter.class})
class JpaPersistenceAdaptersTest {

    @Container
    static GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
        .withEnv("POSTGRES_DB", "sentinel_test")
        .withEnv("POSTGRES_USER", "test")
        .withEnv("POSTGRES_PASSWORD", "test")
        .withExposedPorts(5432)
        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
            "jdbc:postgresql://%s:%d/sentinel_test".formatted(postgres.getHost(), postgres.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private JpaSuspiciousEventRepositoryAdapter eventAdapter;

    @Autowired
    private JpaAlertRepositoryAdapter alertAdapter;

    @PersistenceContext
    private EntityManager entityManager;

    // Postgres guarda microsegundos; truncamos para que equals() no falle por precisión
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private SuspiciousEvent newEvent(ExamSessionId session, EventType type) {
        return new SuspiciousEvent(
            UUID.randomUUID(), session, new StudentId(UUID.randomUUID()),
            new AttemptId(UUID.randomUUID()), type, now()
        );
    }

    private Alert newAlert(ExamSessionId session, List<UUID> sourceEventIds) {
        return new Alert(
            UUID.randomUUID(), session, new StudentId(UUID.randomUUID()),
            new AttemptId(UUID.randomUUID()), AlertSeverity.MEDIUM,
            "Múltiples pérdidas de foco detectadas", sourceEventIds, now()
        );
    }

    // Vacía el caché de Hibernate para que la lectura vaya de verdad a la base de datos
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldRoundTripSuspiciousEvent() {
        ExamSessionId session = new ExamSessionId(UUID.randomUUID());
        SuspiciousEvent event = newEvent(session, EventType.WINDOW_FOCUS_LOST);

        eventAdapter.save(event);
        flushAndClear();

        assertThat(eventAdapter.findByExamSessionId(session)).containsExactly(event);
    }

    @Test
    void shouldOnlyReturnEventsOfTheRequestedSession() {
        ExamSessionId sessionA = new ExamSessionId(UUID.randomUUID());
        ExamSessionId sessionB = new ExamSessionId(UUID.randomUUID());
        SuspiciousEvent eventA = newEvent(sessionA, EventType.WINDOW_FOCUS_LOST);
        SuspiciousEvent eventB = newEvent(sessionB, EventType.TAB_OR_APP_SWITCH);

        eventAdapter.save(eventA);
        eventAdapter.save(eventB);
        flushAndClear();

        assertThat(eventAdapter.findByExamSessionId(sessionA)).containsExactly(eventA);
    }

    @Test
    void shouldRoundTripAlertIncludingSourceEventIds() {
        ExamSessionId session = new ExamSessionId(UUID.randomUUID());
        Alert alert = newAlert(session, List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        alertAdapter.save(alert);
        flushAndClear();

        assertThat(alertAdapter.findByExamSessionId(session)).containsExactly(alert);
    }

    @Test
    void shouldStoreSourceEventIdsAsJsonbArray() {
        ExamSessionId session = new ExamSessionId(UUID.randomUUID());
        Alert alert = newAlert(session, List.of(UUID.randomUUID(), UUID.randomUUID()));

        alertAdapter.save(alert);
        flushAndClear();

        Object jsonType = entityManager
            .createNativeQuery("select jsonb_typeof(source_event_ids) from sentinel_alerts where id = :id")
            .setParameter("id", alert.id())
            .getSingleResult();

        assertThat(jsonType).isEqualTo("array");
    }
}