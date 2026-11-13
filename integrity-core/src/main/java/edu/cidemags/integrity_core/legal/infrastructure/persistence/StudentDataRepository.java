package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas transversales sobre los datos personales del alumno.
 *
 * <p>Se usa JdbcTemplate (no JPA) porque estas consultas cruzan los modulos
 * auth/evaluator/sentinel y no deben acoplar el modulo legal a sus entidades.
 * Todas parametrizan el studentId; ninguna filtra por examen.</p>
 */
@Repository
public class StudentDataRepository {

    private final JdbcTemplate jdbc;

    public StudentDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> findPersonalData(UUID studentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, first_names, last_names, email, role, created_at, is_deleted "
                        + "from users where id = ?", studentId);
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>(rows.get(0));
        data.put("phones", jdbc.queryForList(
                "select phone_number, is_primary from phone_numbers where user_id = ?", studentId));
        return data;
    }

    public List<Map<String, Object>> findResponses(UUID studentId) {
        return jdbc.queryForList("""
                select a.id, at.exam_id, at.id as attempt_id, a.question_id,
                       a.open_text_answer, a.is_deleted
                from exam_answers a
                join exam_attempts at on at.id = a.attempt_id
                where at.student_id = ?
                order by at.started_at desc
                """, studentId);
    }

    public List<Map<String, Object>> findAlerts(UUID studentId) {
        return jdbc.queryForList("""
                select id, exam_id, attempt_id, severity, reason, generated_at, resolved, is_deleted
                from sentinel_alerts
                where student_id = ?
                order by generated_at desc
                """, studentId);
    }

    /** Archiva respuestas anteriores a la fecha (spec 38). Devuelve filas archivadas. */
    public int archiveResponsesBefore(Instant beforeDate) {
        return jdbc.update("""
                insert into archived_responses (id, student_id, exam_id, attempt_id, response_data, archived_at)
                select a.id, at.student_id, at.exam_id, at.id,
                       jsonb_build_object('questionId', a.question_id, 'openTextAnswer', a.open_text_answer),
                       NOW()
                from exam_answers a
                join exam_attempts at on at.id = a.attempt_id
                where at.started_at < ?
                  and a.is_deleted = false
                  and not exists (select 1 from archived_responses ar where ar.id = a.id)
                """, java.sql.Timestamp.from(beforeDate));
    }

    /** Borrado fisico de respuestas anteriores a la fecha (spec 38). */
    public int deleteResponsesBefore(Instant beforeDate) {
        return jdbc.update("""
                delete from exam_answers a
                using exam_attempts at
                where at.id = a.attempt_id
                  and at.started_at < ?
                """, java.sql.Timestamp.from(beforeDate));
    }

    /** Borrado fisico de todas las respuestas del alumno (Art. 17 GDPR, spec 44). */
    public int deleteResponsesByStudent(UUID studentId) {
        return jdbc.update("""
                delete from exam_answers a
                using exam_attempts at
                where at.id = a.attempt_id
                  and at.student_id = ?
                """, studentId);
    }

    public int deleteAlerts(UUID studentId) {
        return jdbc.update("delete from sentinel_alerts where student_id = ?", studentId);
    }

    public int softErase(UUID studentId) {
        int userRows = jdbc.update("update users set is_deleted = true where id = ?", studentId);
        int answerRows = jdbc.update("""
                update exam_answers a set is_deleted = true
                from exam_attempts at
                where at.id = a.attempt_id and at.student_id = ?
                """, studentId);
        int alertRows = jdbc.update(
                "update sentinel_alerts set is_deleted = true where student_id = ?", studentId);
        return userRows + answerRows + alertRows;
    }

    public int anonymizeAuditLogs(UUID studentId) {
        return jdbc.update("""
                update audit_logs
                set actor_id = null, previous_state = null, current_state = null,
                    ip_address = null, user_agent = null
                where actor_id = ?
                """, studentId);
    }

    public boolean hasAnyData(UUID studentId) {
        Integer answers = jdbc.queryForObject("""
                select count(*) from exam_answers a
                join exam_attempts at on at.id = a.attempt_id
                where at.student_id = ? and a.is_deleted = false
                """, Integer.class, studentId);
        Integer alerts = jdbc.queryForObject(
                "select count(*) from sentinel_alerts where student_id = ? and is_deleted = false",
                Integer.class, studentId);
        Integer activeUser = jdbc.queryForObject(
                "select count(*) from users where id = ? and is_deleted = false",
                Integer.class, studentId);
        return (answers != null && answers > 0)
                || (alerts != null && alerts > 0)
                || (activeUser != null && activeUser > 0);
    }
}
