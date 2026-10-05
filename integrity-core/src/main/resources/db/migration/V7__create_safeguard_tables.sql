CREATE TABLE safeguard_configuration (
    id                          UUID PRIMARY KEY,
    exam_id                     UUID        NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    enabled                     BOOLEAN     NOT NULL DEFAULT TRUE,
    require_camera              BOOLEAN     NOT NULL DEFAULT FALSE,
    require_microphone          BOOLEAN     NOT NULL DEFAULT FALSE,
    require_fullscreen          BOOLEAN     NOT NULL DEFAULT FALSE,
    block_copy_paste            BOOLEAN     NOT NULL DEFAULT FALSE,
    max_tab_switches            INTEGER     NOT NULL DEFAULT 3,
    risk_threshold              INTEGER     NOT NULL DEFAULT 100,
    auto_terminate_on_threshold BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_safeguard_config_exam UNIQUE (exam_id),
    CONSTRAINT ck_safeguard_config_tabs CHECK (max_tab_switches >= 0),
    CONSTRAINT ck_safeguard_config_threshold CHECK (risk_threshold > 0)
);

CREATE TABLE safeguard_session (
    id                UUID PRIMARY KEY,
    configuration_id  UUID      NOT NULL REFERENCES safeguard_configuration(id),
    exam_id           UUID        NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    attempt_id        UUID        NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    student_id        UUID        NOT NULL REFERENCES users(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    risk_score        INTEGER     NOT NULL DEFAULT 0,
    started_at        TIMESTAMP WITH TIME ZONE,
    ended_at          TIMESTAMP WITH TIME ZONE,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    client_ip         VARCHAR(45),
    user_agent        VARCHAR(512),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_safeguard_session_attempt UNIQUE (attempt_id),
    CONSTRAINT ck_safeguard_session_status  CHECK (status IN ('PENDING','ACTIVE','PAUSED','COMPLETED','FLAGGED','TERMINATED')),
    CONSTRAINT ck_safeguard_session_risk    CHECK (risk_score >= 0),
    CONSTRAINT ck_safeguard_session_dates   CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_safeguard_session_exam_status ON safeguard_session (exam_id, status);
CREATE INDEX idx_safeguard_session_student     ON safeguard_session (student_id);
CREATE INDEX idx_safeguard_session_config      ON safeguard_session (configuration_id);
CREATE INDEX idx_safeguard_session_flagged     ON safeguard_session (exam_id, risk_score DESC)
    WHERE status IN ('FLAGGED', 'TERMINATED');
CREATE INDEX idx_safeguard_session_heartbeat   ON safeguard_session (last_heartbeat_at)
    WHERE ended_at IS NULL;

CREATE TABLE safeguard_event (
    id          UUID PRIMARY KEY,
    session_id  UUID      NOT NULL REFERENCES safeguard_session(id) ON DELETE CASCADE,
    event_id    UUID        NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    severity    INTEGER    NOT NULL DEFAULT 1,
    risk_points INTEGER     NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata    JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_safeguard_event_session_event UNIQUE (session_id, event_id),
    CONSTRAINT ck_safeguard_event_type CHECK (event_type IN (
        'TAB_SWITCH','WINDOW_BLUR','FULLSCREEN_EXIT','COPY_PASTE','RIGHT_CLICK',
        'MULTIPLE_FACES','NO_FACE_DETECTED','CAMERA_DISABLED','MICROPHONE_DISABLED',
        'NETWORK_DISCONNECT','DEVICE_CHANGE','SUSPICIOUS_ACTIVITY')),
    CONSTRAINT ck_safeguard_event_severity CHECK (severity BETWEEN 1 AND 5),
    CONSTRAINT ck_safeguard_event_points   CHECK (risk_points >= 0)
);

CREATE INDEX idx_safeguard_event_session_time ON safeguard_event (session_id, occurred_at);
CREATE INDEX idx_safeguard_event_session_type ON safeguard_event (session_id, event_type);