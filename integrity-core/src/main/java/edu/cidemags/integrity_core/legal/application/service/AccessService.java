package edu.cidemags.integrity_core.legal.application.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cidemags.integrity_core.legal.domain.entity.AuditLog;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.AuditLogRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derecho de acceso del interesado (Art. 15 GDPR, spec 42). */
@Service
public class AccessService {

    private final StudentDataRepository studentData;
    private final AuditLogRepository auditLogs;
    private final ObjectMapper objectMapper;

    public AccessService(StudentDataRepository studentData, AuditLogRepository auditLogs,
                         ObjectMapper objectMapper) {
        this.studentData = studentData;
        this.auditLogs = auditLogs;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportStudentData(UUID studentId) {
        return buildExport(studentId);
    }

    /** JSON legible (pretty-printed), listo para descargar. */
    @Transactional(readOnly = true)
    public byte[] generateExportFile(UUID studentId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(buildExport(studentId));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el export de datos", ex);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildExport(UUID studentId) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("personalData", studentData.findPersonalData(studentId));
        export.put("responses", studentData.findResponses(studentId));
        export.put("alerts", studentData.findAlerts(studentId));
        export.put("auditTrail", auditTrail(studentId));
        export.put("exportDate", Instant.now().toString());
        return export;
    }

    List<Map<String, Object>> auditTrail(UUID studentId) {
        List<AuditLog> logs = auditLogs.findByActorIdOrderByOccurredAtAsc(studentId);
        return logs.stream().map(log -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("action", log.getAction());
            row.put("entityType", log.getEntityType());
            row.put("entityId", log.getEntityId());
            row.put("occurredAt", log.getOccurredAt());
            return row;
        }).toList();
    }
}
