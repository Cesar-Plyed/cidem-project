package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import edu.cidemags.integrity_core.evaluator.domain.port.ExamParticipationRepository;

@Repository
public class ExamParticipationRepositoryImpl implements ExamParticipationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExamParticipationRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean reserve(UUID examId, UUID studentId) {
        return jdbcTemplate.update("""
                INSERT INTO exam_student_participation (exam_id, student_id)
                VALUES (?, ?)
                ON CONFLICT (exam_id, student_id) DO NOTHING
                """, examId, studentId) == 1;
    }
}
