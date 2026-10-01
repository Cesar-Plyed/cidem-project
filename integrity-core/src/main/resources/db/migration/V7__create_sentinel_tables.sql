CREATE TABLE sentinel_suspicious_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('WINDOW_FOCUS_LOST', 'WINDOW_FOCUS_REGAINED', 'TAB_OR_APP_SWITCH')),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sentinel_events_session_time ON sentinel_suspicious_events(exam_session_id, occurred_at);
CREATE INDEX idx_sentinel_events_attempt ON sentinel_suspicious_events(attempt_id);

CREATE TABLE sentinel_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    reason VARCHAR(500) NOT NULL,
    source_event_ids JSONB NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sentinel_alerts_session_time ON sentinel_alerts(exam_session_id, generated_at);
CREATE INDEX idx_sentinel_alerts_attempt ON sentinel_alerts(attempt_id);