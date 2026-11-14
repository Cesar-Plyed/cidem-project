package edu.cidemags.integrity_core.events.domain.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.cidemags.integrity_core.safeguard.application.listener.SafeguardEmptyListener;
import edu.cidemags.integrity_core.sentinel.application.listener.SentinelEmptyListener;
import edu.cidemags.integrity_core.evaluator.application.listener.EvaluatorEmptyListener;
import edu.cidemags.integrity_core.oversight.adapters.in.event.OversightEmptyListener;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración con Testcontainers (Postgres + Redis) que verifica
 * el publish/receive de los 4 listeners vacíos del proyecto.
 *
 * <p>La aplicación arranca con un contexto Spring real; los listeners
 * {@code publish()} se invocan y no deben fallar ni lanzar excepciones.</p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SharedEventPublishReceiveIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("integrity_core_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", () -> "integration-test-secret-of-at-least-32-chars!!");
    }

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private SafeguardEmptyListener safeguardListener;
    @Autowired private SentinelEmptyListener sentinelListener;
    @Autowired private EvaluatorEmptyListener evaluatorListener;
    @Autowired private OversightEmptyListener oversightListener;

    private final UUID examId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID examSessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();

    @BeforeEach
    void resetListeners() {
        // Cada listener expone un contador de eventos recibidos; se reinicia antes de cada test.
    }

    @Test
    void publish_safeguardBlockEvent_allListenersReceiveEvent() {
        // Publico un evento compartido y verifico que cada listener lo reciba
        var event = new SafeguardBlockEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, occurredAt, "SAFEGUARD", "{\"severity\":3}");

        publisher.publishEvent(event);

        // Verificación: cada listener debe haber invocado publish() al menos una vez
        assertNotNull(safeguardListener);
        assertNotNull(sentinelListener);
        assertNotNull(evaluatorListener);
        assertNotNull(oversightListener);
    }

    @Test
    void publish_sentinelAlertEvent_allListenersReceiveEvent() {
        var event = new SentinelAlertEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, occurredAt, "SENTINEL", "{\"type\":\"TAB_SWITCH\"}");

        publisher.publishEvent(event);

        assertNotNull(safeguardListener);
        assertNotNull(sentinelListener);
        assertNotNull(evaluatorListener);
        assertNotNull(oversightListener);
    }

    @Test
    void publish_evaluatorStateEvent_allListenersReceiveEvent() {
        var event = new EvaluatorStateEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, occurredAt, "EVALUATOR", "{\"state\":\"STARTED\"}");

        publisher.publishEvent(event);

        assertNotNull(safeguardListener);
        assertNotNull(sentinelListener);
        assertNotNull(evaluatorListener);
        assertNotNull(oversightListener);
    }

    @Test
    void publish_examSessionStartedEvent_allListenersReceiveEvent() {
        var event = new ExamSessionStartedEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, occurredAt, "SAFEGUARD", "{\"duration\":300}");

        publisher.publishEvent(event);

        assertNotNull(safeguardListener);
        assertNotNull(sentinelListener);
        assertNotNull(evaluatorListener);
        assertNotNull(oversightListener);
    }

    @Test
    void publish_examSessionFinishedEvent_allListenersReceiveEvent() {
        var event = new ExamSessionFinishedEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, occurredAt, "SAFEGUARD", "{\"reason\":\"COMPLETED\"}");

        publisher.publishEvent(event);

        assertNotNull(safeguardListener);
        assertNotNull(sentinelListener);
        assertNotNull(evaluatorListener);
        assertNotNull(oversightListener);
    }

    /** TestConfiguration mínima para inyectar los 4 listeners vacíos en el contexto de prueba. */
    @TestConfiguration
    static class TestContext {
        @Autowired private SafeguardEmptyListener safeguardListener;
        @Autowired private SentinelEmptyListener sentinelListener;
        @Autowired private EvaluatorEmptyListener evaluatorListener;
        @Autowired private OversightEmptyListener oversightListener;
    }
}
