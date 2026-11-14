package edu.cidemags.integrity_core.events.domain.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que las 5 shared-event records se serializan/deserializan correctamente
 * con Jackson (Java 25 records, JSR-310 Instant).
 */
class SharedEventsSerialisationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final UUID examId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID examSessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private void init() {
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
    }

    @Test
    void safeguardBlockEvent_serialisesAndDeserialises() throws JsonProcessingException {
        init();

        SafeguardBlockEvent original = new SafeguardBlockEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SAFEGUARD", "{\"reason\":\"tab_switch\"}");

        String json = mapper.writeValueAsString(original);
        assertNotNull(json);

        SafeguardBlockEvent deserialized = mapper.readValue(json, SafeguardBlockEvent.class);
        assertEquals(original.eventId(), deserialized.eventId());
        assertEquals(original.examId(), deserialized.examId());
        assertEquals(original.attemptId(), deserialized.attemptId());
        assertEquals(original.examSessionId(), deserialized.examSessionId());
        assertEquals(original.studentId(), deserialized.studentId());
        assertEquals(original.occurredAt(), deserialized.occurredAt());
        assertEquals(original.source(), deserialized.source());
        assertEquals(original.payload(), deserialized.payload());
    }

    @Test
    void sentinelAlertEvent_serialisesAndDeserialises() throws JsonProcessingException {
        init();

        SentinelAlertEvent original = new SentinelAlertEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SENTINEL", "{\"type\":\"TAB_OR_APP_SWITCH\"}");

        String json = mapper.writeValueAsString(original);
        SentinelAlertEvent deserialized = mapper.readValue(json, SentinelAlertEvent.class);
        assertEquals(original.eventId(), deserialized.eventId());
        assertEquals(original.occurredAt(), deserialized.occurredAt());
    }

    @Test
    void evaluatorStateEvent_serialisesAndDeserialises() throws JsonProcessingException {
        init();

        EvaluatorStateEvent original = new EvaluatorStateEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "EVALUATOR", "{\"state\":\"STARTED\"}");

        String json = mapper.writeValueAsString(original);
        EvaluatorStateEvent deserialized = mapper.readValue(json, EvaluatorStateEvent.class);
        assertEquals(original.occurredAt(), deserialized.occurredAt());
    }

    @Test
    void examSessionStartedEvent_serialisesAndDeserialises() throws JsonProcessingException {
        init();

        ExamSessionStartedEvent original = new ExamSessionStartedEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SAFEGUARD", "{\"duration\":120}");

        String json = mapper.writeValueAsString(original);
        ExamSessionStartedEvent deserialized = mapper.readValue(json, ExamSessionStartedEvent.class);
        assertEquals(original.occurredAt(), deserialized.occurredAt());
    }

    @Test
    void examSessionFinishedEvent_serialisesAndDeserialises() throws JsonProcessingException {
        init();

        ExamSessionFinishedEvent original = new ExamSessionFinishedEvent(
                UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SAFEGUARD", "{\"reason\":\"COMPLETED\"}");

        String json = mapper.writeValueAsString(original);
        ExamSessionFinishedEvent deserialized = mapper.readValue(json, ExamSessionFinishedEvent.class);
        assertEquals(original.occurredAt(), deserialized.occurredAt());
    }

    @Test
    void allEvents_shareCommonFields() throws JsonProcessingException {
        init();

        var events = List.of(
                new SafeguardBlockEvent(UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SAFEGUARD", "{}"),
                new SentinelAlertEvent(UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SENTINEL", "{}"),
                new EvaluatorStateEvent(UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "EVALUATOR", "{}"),
                new ExamSessionStartedEvent(UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SAFEGUARD", "{}"),
                new ExamSessionFinishedEvent(UUID.randomUUID(), examId, attemptId, examSessionId, studentId, now, "SAFEGUARD", "{}")
        );

        for (var event : events) {
            // Jackson deserializa con el tipo estático `Class<? extends Record>`,
            // así que se comprueban los campos comunes sobre el JSON resultante.
            var node = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals(examId.toString(), node.get("examId").asText());
            assertEquals(attemptId.toString(), node.get("attemptId").asText());
            assertEquals(examSessionId.toString(), node.get("examSessionId").asText());
            assertEquals(studentId.toString(), node.get("studentId").asText());
            assertEquals(now.getEpochSecond(), (long) node.get("occurredAt").asDouble());
        }
    }
}
