package edu.cidemags.integrity_core.sentinel.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cidemags.integrity_core.events.domain.event.SentinelAlertEvent;
import edu.cidemags.integrity_core.sentinel.application.event.AlertGeneratedEvent;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SentinelEventPublisherTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ApplicationEventPublisher publisher;
    private SentinelEventPublisher eventPublisher;

    private final UUID examId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID examSessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        publisher = mock(ApplicationEventPublisher.class);
        eventPublisher = new SentinelEventPublisher(redisTemplate, publisher, new ObjectMapper());
    }

    private Alert alert() {
        return new Alert(
            UUID.randomUUID(),
            new ExamSessionId(examSessionId),
            new StudentId(studentId),
            new AttemptId(attemptId),
            AlertSeverity.HIGH,
            "Cambio de pestaña detectado",
            List.of(UUID.randomUUID()),
            Instant.now(),
            "TAB_SWITCH",
            examId,
            false
        );
    }

    @Test
    void shouldPublishWhenAlertWasNotPublishedBefore() {
        when(valueOperations.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(true);

        boolean published = eventPublisher.publish(alert());

        assertThat(published).isTrue();
        verify(publisher).publishEvent(any(AlertGeneratedEvent.class));
        verify(publisher).publishEvent(any(SentinelAlertEvent.class));
    }

    @Test
    void shouldSilenceDuplicateWithinTheFiveMinuteWindow() {
        when(valueOperations.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(false);

        boolean published = eventPublisher.publish(alert());

        assertThat(published).isFalse();
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishAgainOnceTheWindowExpired() {
        // Primera publicacion: la clave no existia.
        when(valueOperations.setIfAbsent(any(), eq("1"), any(Duration.class)))
            .thenReturn(true)
            .thenReturn(true);

        assertThat(eventPublisher.publish(alert())).isTrue();
        // Simula que Redis expiro la clave y la alerta vuelve a publicarse.
        assertThat(eventPublisher.publish(alert())).isTrue();
    }

    @Test
    void shouldUseFiveMinuteTtlAndCompositeKey() {
        when(valueOperations.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(true);

        eventPublisher.publish(alert());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("1"), ttlCaptor.capture());

        assertThat(keyCaptor.getValue())
            .isEqualTo("dedup:TAB_SWITCH:" + examId + ":" + attemptId);
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));
    }
}
