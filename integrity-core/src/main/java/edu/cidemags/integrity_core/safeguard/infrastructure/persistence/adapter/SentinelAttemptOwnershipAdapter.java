package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.safeguard.domain.port.ExamAttemptPort;
import edu.cidemags.integrity_core.sentinel.ports.in.AttemptOwnershipPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementa el port de sentinel reutilizando EXACTAMENTE la consulta de
 * JdbcExamAttemptAdapter: la verificación de propiedad (exam_attempts.student_id)
 * es la misma para SafeGuard y para Sentinel, pero el port vive en módulos
 * distintos (cada módulo declara el suyo, sin imports cruzados).
 */
@Component
public class SentinelAttemptOwnershipAdapter implements AttemptOwnershipPort {

    private final JdbcTemplate jdbc;

    public SentinelAttemptOwnershipAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttemptOwner> findOwner(UUID attemptId) {
        return jdbc.query(
                "SELECT id, exam_id, student_id FROM exam_attempts WHERE id = ?",
                (rs, i) -> new AttemptOwner(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exam_id", UUID.class),
                        rs.getObject("student_id", UUID.class)),
                attemptId).stream().findFirst();
    }
}
