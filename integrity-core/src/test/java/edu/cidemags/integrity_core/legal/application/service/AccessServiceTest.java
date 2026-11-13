package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.AuditLogRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private StudentDataRepository studentData;

    @Mock
    private AuditLogRepository auditLogs;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void exportStudentDataContainsAllRequiredSections() {
        when(studentData.findPersonalData(studentId)).thenReturn(Map.of("email", "a@b.c"));
        when(studentData.findResponses(studentId)).thenReturn(List.of(Map.of("id", "r1")));
        when(studentData.findAlerts(studentId)).thenReturn(List.of(Map.of("id", "a1")));
        when(auditLogs.findByActorIdOrderByOccurredAtAsc(studentId)).thenReturn(List.of());

        var export = new AccessService(studentData, auditLogs, new ObjectMapper()).exportStudentData(studentId);

        assertThat(export).containsKeys("personalData", "responses", "alerts", "auditTrail", "exportDate");
    }

    @Test
    void exportFileIsPrettyPrintedJson() {
        when(studentData.findPersonalData(studentId)).thenReturn(Map.of("email", "a@b.c"));
        when(studentData.findResponses(studentId)).thenReturn(List.of());
        when(studentData.findAlerts(studentId)).thenReturn(List.of());
        when(auditLogs.findByActorIdOrderByOccurredAtAsc(studentId)).thenReturn(List.of());

        byte[] file = new AccessService(studentData, auditLogs, new ObjectMapper())
                .generateExportFile(studentId);
        String json = new String(file, StandardCharsets.UTF_8);

        assertThat(json).contains("\n", "\"personalData\"", "\"exportDate\"");
    }
}
