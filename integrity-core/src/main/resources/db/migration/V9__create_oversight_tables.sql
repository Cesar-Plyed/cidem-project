CREATE TABLE oversight_log_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    source VARCHAR(20) NOT NULL CHECK (source IN ('SENTINEL', 'SAFEGUARD', 'EVALUATOR')),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    description VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_oversight_log_entries_session ON oversight_log_entries(exam_session_id, occurred_at);
CREATE INDEX idx_oversight_log_entries_attempt ON oversight_log_entries(attempt_id);

CREATE TABLE oversight_integrity_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_oversight_snapshot_attempt UNIQUE (attempt_id)
);

CREATE INDEX idx_oversight_snapshots_session ON oversight_integrity_snapshots(exam_session_id);