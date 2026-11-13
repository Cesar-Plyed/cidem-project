package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.cidemags.integrity_core.legal.domain.entity.ConsentLog;
import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ConsentLogRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortabilityServiceTest {

    @Mock
    private StudentDataRepository studentData;

    @Mock
    private ConsentLogRepository consentLogs;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void jsonContainsAllPortableSections() {
        when(studentData.findPersonalData(studentId)).thenReturn(Map.of("email", "a@b.c"));
        when(studentData.findResponses(studentId)).thenReturn(List.of());
        when(studentData.findAlerts(studentId)).thenReturn(List.of());
        when(consentLogs.findByStudentIdOrderByCreatedAtAsc(studentId))
                .thenReturn(List.of(ConsentLog.grant(studentId, ConsentType.CAMERA, null, null)));

        String json = new String(new PortabilityService(studentData, consentLogs, jsonMapper())
                .exportAsJson(studentId), StandardCharsets.UTF_8);

        assertThat(json).contains("personalData", "responses", "alerts", "consentHistory");
    }

    @Test
    void csvHasHeadersAndEscapesCommas() {
        when(studentData.findPersonalData(studentId))
                .thenReturn(Map.of("email", "a@b.c", "firstNames", "Ana, Maria"));
        when(studentData.findResponses(studentId)).thenReturn(List.of(Map.of("id", "r1", "open_text_answer", "ok")));
        when(studentData.findAlerts(studentId)).thenReturn(List.of());

        String csv = new String(new PortabilityService(studentData, consentLogs, jsonMapper())
                .exportAsCsv(studentId), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("section,item_id,field,value\n");
        assertThat(csv).contains("personalData,,firstNames,\"Ana, Maria\"");
        assertThat(csv).contains("responses,r1,open_text_answer,ok");
    }

    private static ObjectMapper jsonMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
