package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.safeguard.domain.port.ExamAttemptPort;
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

    @Override
    @Transactional(readOnly = true)
    public boolean attemptExistsForStudent(UUID examId, UUID studentId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM exam_attempts WHERE exam_id = ? AND student_id = ?)",
                Boolean.class, examId, studentId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void failAttemptForIntegrity(UUID attemptId) {
        jdbc.update(
                "UPDATE exam_attempts SET submitted = TRUE, "
                        + "submitted_at = COALESCE(submitted_at, CURRENT_TIMESTAMP), integrity_failed = TRUE "
                        + "WHERE id = ?",
                attemptId);
    }
}
