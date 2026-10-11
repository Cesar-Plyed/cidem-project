package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import java.util.UUID;

import edu.cidemags.integrity_core.sentinel.ports.in.ExamOversightAccessPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExamOversightAccessAdapter implements ExamOversightAccessPort {

    private final JdbcTemplate jdbc;

    public ExamOversightAccessAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageExam(UUID examId, UUID requesterId) {
        Boolean ownsExam = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM exams WHERE id = ? AND created_by_user_id = ?)",
                Boolean.class, examId, requesterId);
        return Boolean.TRUE.equals(ownsExam);
    }
}
