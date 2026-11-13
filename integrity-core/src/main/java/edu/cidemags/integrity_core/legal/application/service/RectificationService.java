package edu.cidemags.integrity_core.legal.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derecho de rectificacion (Art. 16 GDPR, spec 43). */
@Service
public class RectificationService {

    /** Whitelist entityType -> (campo API -> columna SQL). Evita SQL injection. */
    private static final Map<String, String> USER_COLUMNS = Map.of(
            "email", "email",
            "firstNames", "first_names",
            "lastNames", "last_names");

    private static final Map<String, String> TABLE_BY_TYPE = Map.of(
            "USER", "users",
            "STUDENT", "users",
            "EMAIL", "users",
            "PHONE", "phone_numbers",
            "RESPONSE", "exam_answers",
            "EXAM_ANSWER", "exam_answers",
            "ALERT", "sentinel_alerts");

    private static final Set<String> SOFT_DELETE_TABLES = Set.of(
            "users", "exam_answers", "sentinel_alerts");

    private final JdbcTemplate jdbc;

    public RectificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int rectifyData(String entityType, UUID entityId, String fieldName, Object newValue) {
        String table = resolveTable(entityType);
        String column = resolveColumn(entityType, fieldName);
        return jdbc.update("update " + table + " set " + column + " = ? where id = ?",
                newValue, entityId);
    }

    @Transactional
    public int softDeleteData(String entityType, UUID entityId) {
        String table = resolveTable(entityType);
        validateSoftDeleteTable(table);
        return jdbc.update("update " + table + " set is_deleted = true where id = ?", entityId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> currentData(String entityType, UUID entityId) {
        String table = resolveTable(entityType);
        return jdbc.queryForList("select * from " + table + " where id = ?", entityId);
    }

    private static String resolveTable(String entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType es obligatorio");
        }
        String table = TABLE_BY_TYPE.get(entityType.toUpperCase());
        if (table == null) {
            throw new IllegalArgumentException("entityType no soportado: " + entityType);
        }
        return table;
    }

    private static String resolveColumn(String entityType, String fieldName) {
        String type = entityType.toUpperCase();
        if ("USER".equals(type) || "STUDENT".equals(type) || "EMAIL".equals(type)) {
            if ("EMAIL".equals(type)) {
                return "email";
            }
            String column = USER_COLUMNS.get(fieldName);
            if (column == null) {
                throw new IllegalArgumentException("Campo no rectificable: " + fieldName);
            }
            return column;
        }
        if ("PHONE".equals(type) && "phoneNumber".equals(fieldName)) {
            return "phone_number";
        }
        if (("RESPONSE".equals(type) || "EXAM_ANSWER".equals(type))
                && "openTextAnswer".equals(fieldName)) {
            return "open_text_answer";
        }
        if ("ALERT".equals(type) && "reason".equals(fieldName)) {
            return "reason";
        }
        throw new IllegalArgumentException("Campo no rectificable: " + entityType + "." + fieldName);
    }

    private static void validateSoftDeleteTable(String table) {
        if (!SOFT_DELETE_TABLES.contains(table)) {
            throw new IllegalArgumentException("La entidad no soporta borrado logico: " + table);
        }
    }
}
