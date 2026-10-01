package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.cidemags.integrity_core.sentinel.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisEventWindowAdapterTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final AttemptId attemptId = new AttemptId(UUID.randomUUID());

    private RedisEventWindowAdapter adapter;

    @BeforeEach
    void setUp() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(
        redis.getHost(), redis.getMappedPort(6379)
    );
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
    connectionFactory.afterPropertiesSet();

    StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();

    adapter = new RedisEventWindowAdapter(redisTemplate);
    }

    @Test
    void shouldRoundTripSuspiciousEventThroughRedis() {
        SuspiciousEvent event = new SuspiciousEvent(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            EventType.WINDOW_FOCUS_LOST, Instant.now()
        );

        adapter.record(event);

        List<SuspiciousEvent> found = adapter.findRecentEvents(
            examSessionId, studentId, EventType.WINDOW_FOCUS_LOST, Duration.ofMinutes(2)
        );

        assertThat(found).hasSize(1);
        assertThat(found.get(0)).isEqualTo(event);
    }

    @Test
    void shouldNotReturnEventsOutsideTheRequestedWindow() {
        SuspiciousEvent oldEvent = new SuspiciousEvent(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            EventType.WINDOW_FOCUS_LOST, Instant.now().minus(Duration.ofMinutes(4))
        );

        adapter.record(oldEvent);

        List<SuspiciousEvent> found = adapter.findRecentEvents(
            examSessionId, studentId, EventType.WINDOW_FOCUS_LOST, Duration.ofMinutes(2)
        );

        assertThat(found).isEmpty();
    }

    @Test
    void shouldIsolateWindowsByEventType() {
        SuspiciousEvent focusLostEvent = new SuspiciousEvent(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            EventType.WINDOW_FOCUS_LOST, Instant.now()
        );

        adapter.record(focusLostEvent);

        List<SuspiciousEvent> tabSwitchResults = adapter.findRecentEvents(
            examSessionId, studentId, EventType.TAB_OR_APP_SWITCH, Duration.ofMinutes(2)
        );

        assertThat(tabSwitchResults).isEmpty();
    }
}