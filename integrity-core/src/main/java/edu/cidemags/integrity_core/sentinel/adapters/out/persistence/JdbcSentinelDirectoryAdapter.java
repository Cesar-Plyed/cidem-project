package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.ports.out.SentinelDirectoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSentinelDirectoryAdapter implements SentinelDirectoryPort {

    private final JdbcTemplate jdbc;

    public JdbcSentinelDirectoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findStudentName(UUID studentId) {
        return jdbc.query(
                "SELECT first_names, last_names FROM users WHERE id = ?",
                (rs, i) -> (rs.getString("first_names") + " " + rs.getString("last_names")).trim(),
                studentId)
            .stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findExamName(UUID examId) {
        return jdbc.query(
                "SELECT title FROM exams WHERE id = ?",
                (rs, i) -> rs.getString("title"),
                examId)
            .stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findExamIdBySession(UUID examSessionId) {
        Optional<UUID> fromAlerts = jdbc.query(
                "SELECT exam_id FROM sentinel_alerts "
                    + "WHERE exam_session_id = ? AND exam_id IS NOT NULL LIMIT 1",
                (rs, i) -> rs.getObject("exam_id", UUID.class),
                examSessionId)
            .stream().findFirst();

        if (fromAlerts.isPresent()) {
            return fromAlerts;
        }

        return jdbc.query(
                "SELECT exam_id FROM sentinel_suspicious_events "
                    + "WHERE exam_session_id = ? AND exam_id IS NOT NULL LIMIT 1",
                (rs, i) -> rs.getObject("exam_id", UUID.class),
                examSessionId)
            .stream().findFirst();
    }
}
