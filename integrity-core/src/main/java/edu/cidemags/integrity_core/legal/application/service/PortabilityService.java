package edu.cidemags.integrity_core.legal.application.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cidemags.integrity_core.legal.domain.entity.ConsentLog;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ConsentLogRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derecho a la portabilidad (Art. 20 GDPR, spec 45). */
@Service
public class PortabilityService {

    private final StudentDataRepository studentData;
    private final ConsentLogRepository consentLogs;
    private final ObjectMapper objectMapper;

    public PortabilityService(StudentDataRepository studentData, ConsentLogRepository consentLogs,
                              ObjectMapper objectMapper) {
        this.studentData = studentData;
        this.consentLogs = consentLogs;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public byte[] exportAsJson(UUID studentId) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("personalData", studentData.findPersonalData(studentId));
        export.put("responses", studentData.findResponses(studentId));
        export.put("alerts", studentData.findAlerts(studentId));
        export.put("consentHistory", consentHistory(studentId));
        export.put("exportDate", Instant.now().toString());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la exportacion JSON", ex);
        }
    }

    /**
     * CSV plano y legible por humanos con secciones personalData/responses/alerts.
     * Las comas, comillas y saltos de linea se escapan segun RFC 4180.
     */
    @Transactional(readOnly = true)
    public byte[] exportAsCsv(UUID studentId) {
        StringBuilder csv = new StringBuilder();
        csv.append("section,item_id,field,value\n");

        Map<String, Object> personal = studentData.findPersonalData(studentId);
        personal.forEach((field, value) -> appendRow(csv, "personalData", "", field, value));

        List<Map<String, Object>> responses = studentData.findResponses(studentId);
        for (Map<String, Object> response : responses) {
            Object id = response.get("id");
            response.forEach((field, value) -> {
                if (!"id".equals(field)) {
                    appendRow(csv, "responses", String.valueOf(id), field, value);
                }
            });
        }

        List<Map<String, Object>> alerts = studentData.findAlerts(studentId);
        for (Map<String, Object> alert : alerts) {
            Object id = alert.get("id");
            alert.forEach((field, value) -> {
                if (!"id".equals(field)) {
                    appendRow(csv, "alerts", String.valueOf(id), field, value);
                }
            });
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> consentHistory(UUID studentId) {
        List<ConsentLog> logs = consentLogs.findByStudentIdOrderByCreatedAtAsc(studentId);
        List<Map<String, Object>> history = new ArrayList<>();
        for (ConsentLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", log.getConsentType().name());
            row.put("action", log.isGranted() ? "granted" : "revoked");
            row.put("timestamp", log.effectiveAt());
            history.add(row);
        }
        return history;
    }

    private static void appendRow(StringBuilder csv, String section, String itemId,
                                  String field, Object value) {
        csv.append(escape(section)).append(',')
                .append(escape(itemId)).append(',')
                .append(escape(field)).append(',')
                .append(escape(value == null ? "" : String.valueOf(value))).append('\n');
    }

    static String escape(String value) {
        String raw = value == null ? "" : value;
        if (raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")) {
            return '"' + raw.replace("\"", "\"\"") + '"';
        }
        return raw;
    }
}
