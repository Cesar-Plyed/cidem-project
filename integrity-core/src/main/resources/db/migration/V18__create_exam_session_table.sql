-- V18: tabla de sesiones de examen persistidas en PostgreSQL.
--
-- El spec 24-31 introdujo RedisSessionEntity (@Table "exam_session") para
-- guardar el estado durable de cada sesión, con copia en Redis con TTL.
-- La migración no se creó en su momento, así que Hibernate (ddl-auto=validate)
-- abortaba el arranque con "missing table [exam_session]".

CREATE TABLE IF NOT EXISTS exam_session (
    id                   UUID PRIMARY KEY,
    exam_id              UUID         NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    attempt_id           UUID         NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    student_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status               VARCHAR(20)  NOT NULL,
    started_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at          TIMESTAMP WITH TIME ZONE,
    max_duration_seconds BIGINT       NOT NULL,
    payload              VARCHAR(1000),
    CONSTRAINT ck_exam_session_status CHECK (status IN (
        'RUNNING', 'PAUSED', 'COMPLETED', 'BLOCKED', 'EXPIRED')),
    CONSTRAINT ck_exam_session_duration CHECK (max_duration_seconds > 0)
);

CREATE INDEX IF NOT EXISTS idx_exam_session_exam ON exam_session (exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_session_attempt ON exam_session (attempt_id);
CREATE INDEX IF NOT EXISTS idx_exam_session_student ON exam_session (student_id);
CREATE INDEX IF NOT EXISTS idx_exam_session_status ON exam_session (status);
