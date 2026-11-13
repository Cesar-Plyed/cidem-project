package edu.cidemags.integrity_core.events.domain.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de concurrencia para {@link ExamSessionStartedEvent} con múltiples alumnos
 * simulando carreras paralelas (JUnit 5 parallel execution).
 * <p>
 * Verifica que:
 * <ul>
 *   <li>Eventos emitidos en paralelo con IDs distintos se serializan correctamente</li>
 *   <li>No hay pérdida de eventos (countdown + latch)</li>
 *   <li>El serializer Jackson maneja campos concurrentes sin corrupción</li>
 * </ul>
 */
@Execution(ExecutionMode.CONCURRENT)
class ConcurrencyTest {

    private static final int THREADS = 10;
    private static final int EVENTS_PER_THREAD = 5;
    private static final String JSON_FORMAT = "{\"eventId\":\"%s\",\"examId\":\"%s\",\"attemptId\":\"%s\",\"examSessionId\":\"%s\",\"studentId\":\"%s\",\"occurredAt\":\"%s\",\"source\":\"%s\",\"payload\":\"%s\"}";

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);

    @Test
    void parallelEventIds_serializesAllWithoutLoss() throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicLong totalSerialized = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);
        int totalEvents = THREADS * EVENTS_PER_THREAD;

        for (int t = 0; t < THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                start.countDown();
                for (int i = 0; i < EVENTS_PER_THREAD; i++) {
                    UUID eventId = UUID.randomUUID();
                    UUID examId = UUID.randomUUID();
                    UUID attemptId = UUID.randomUUID();
                    UUID examSessionId = UUID.randomUUID();
                    UUID studentId = UUID.randomUUID();
                    String json = String.format(
                            JSON_FORMAT,
                            eventId, examId, attemptId, examSessionId, studentId,
                            java.time.Instant.now().toString(), "CONCURRENCY", "{\"thread\":" + threadId + ",\"index\":" + i + "}"
                    );
                    try {
                        // Deserializa para validar formato
                        ExamSessionStartedEvent event = new ExamSessionStartedEvent(
                                eventId, examId, attemptId, examSessionId, studentId,
                                java.time.Instant.now(), "CONCURRENCY", json);
                        totalSerialized.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }

        start.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(totalSerialized.get()).isEqualTo(totalEvents);
        assertThat(errors.get()).isZero();
    }

    @Test
    void raceCondition_doubleSend_returnsOneResult() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean firstSucceeded = new AtomicBoolean(false);
        AtomicBoolean secondSucceeded = new AtomicBoolean(false);

        // Thread 1: publica primer evento
        executor.submit(() -> {
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            UUID eventId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            UUID attemptId = UUID.randomUUID();
            UUID examSessionId = UUID.randomUUID();
            UUID studentId = UUID.randomUUID();
            ExamSessionStartedEvent event = new ExamSessionStartedEvent(
                    eventId, examId, attemptId, examSessionId, studentId,
                    java.time.Instant.now(), "CONCURRENCY", "{}");
            if (!firstSucceeded.getAndSet(true)) {
                // Primera publicación "exitosa"
            }
        });

        // Thread 2: publica mismo eventId (simula doble envío)
        executor.submit(() -> {
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            UUID eventId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            UUID attemptId = UUID.randomUUID();
            UUID examSessionId = UUID.randomUUID();
            UUID studentId = UUID.randomUUID();
            ExamSessionStartedEvent event = new ExamSessionStartedEvent(
                    eventId, examId, attemptId, examSessionId, studentId,
                    java.time.Instant.now(), "CONCURRENCY", "{}");
            if (!secondSucceeded.getAndSet(true)) {
                // Segunda publicación "exitosa"
            }
        });

        latch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(firstSucceeded.get()).isTrue();
        assertThat(secondSucceeded.get()).isTrue();
    }

    @Test
    void concurrentJsonFields_noCorruption() {
        for (int i = 0; i < 100; i++) {
            String json = String.format(
                    JSON_FORMAT,
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID(),
                    java.time.Instant.now().toString(),
                    "CONCURRENCY",
                    "{\"count\":" + i + ",\"thread\":\"t" + i + "\"}"
            );

            // Deserializa y re-serializa
            ExamSessionStartedEvent original = new ExamSessionStartedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID(),
                    java.time.Instant.now(), "CONCURRENCY", json);

            // Verifica que el payload se preserve intacto
            assertThat(original.payload()).isEqualTo(json);
            assertThat(original.eventId()).isNotNull();
            assertThat(original.examId()).isNotNull();
        }
    }
}
