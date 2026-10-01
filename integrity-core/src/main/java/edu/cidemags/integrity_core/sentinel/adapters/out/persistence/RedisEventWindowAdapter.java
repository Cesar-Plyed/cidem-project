package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.cidemags.integrity_core.sentinel.domain.EventType;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.sentinel.ports.out.EventWindowPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RedisEventWindowAdapter implements EventWindowPort {

    private static final Duration RETENTION_WINDOW = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEventWindowAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void record(SuspiciousEvent event) {
        String key = buildKey(event.examSessionId(), event.studentId(), event.type());
        double score = event.occurredAt().toEpochMilli();

        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForZSet().add(key, json, score);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento sospechoso", e);
        }

        double cutoff = Instant.now().minus(RETENTION_WINDOW).toEpochMilli();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
    }

    @Override
    public List<SuspiciousEvent> findRecentEvents(
        ExamSessionId examSessionId, StudentId studentId, EventType type, Duration window
    ) {
        String key = buildKey(examSessionId, studentId, type);
        double from = Instant.now().minus(window).toEpochMilli();
        double to = Instant.now().toEpochMilli();

        Set<String> rawEvents = redisTemplate.opsForZSet().rangeByScore(key, from, to);

        if (rawEvents == null) {
            return List.of();
        }

        return rawEvents.stream()
            .map(this::deserialize)
            .collect(Collectors.toList());
    }

    private SuspiciousEvent deserialize(String json) {
        try {
            return objectMapper.readValue(json, SuspiciousEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo deserializar un evento sospechoso desde Redis", e);
        }
    }

    private String buildKey(ExamSessionId examSessionId, StudentId studentId, EventType type) {
        return "sentinel:events:%s:%s:%s".formatted(
            examSessionId.value(), studentId.value(), type.name()
        );
    }
}