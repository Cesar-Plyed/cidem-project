package edu.cidemags.integrity_core.safeguard.infrastructure;

import edu.cidemags.integrity_core.safeguard.service.ExamAttemptPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcExamAttemptAdapter implements ExamAttemptPort {

    private final JdbcTemplate jdbc;

    public JdbcExamAttemptAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean examExists(UUID examId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM exams WHERE id = ?)", Boolean.class, examId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttemptRef> findAttempt(UUID attemptId) {
        return jdbc.query(
                "SELECT id, exam_id, student_id FROM exam_attempts WHERE id = ?",
                (rs, i) -> new AttemptRef(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exam_id", UUID.class),
                        rs.getObject("student_id", UUID.class)),
                attemptId).stream().findFirst();
    }
}
