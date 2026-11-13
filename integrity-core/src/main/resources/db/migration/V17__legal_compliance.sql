-- V17: cumplimiento legal y proteccion de datos (specs 36-50).
--
-- Los specs proponian V005/V006/V007, pero en este repositorio la ultima
-- migracion aplicada es V16. Se agrupan todas las tablas legales en una sola
-- migracion aditiva para no renumerar el historial de Flyway.
--
-- Nota: consent_logs y audit_logs son append-only. Se protegen con reglas de
-- PostgreSQL (UPDATE/DELETE -> DO INSTEAD NOTHING) para que sean inmutables a
-- nivel de base de datos, no solo por convencion de codigo.

-- ---------------------------------------------------------------------------
-- 36 / 40 / 48: consentimiento (LOPDGDD art. 6-7)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS consent_logs (
    id              UUID PRIMARY KEY,
    student_id      UUID        NOT NULL,
    consent_type    VARCHAR(30) NOT NULL,
    granted_at      TIMESTAMP WITH TIME ZONE,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    ip              VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_consent_logs_type CHECK (consent_type IN (
        'CAMERA', 'MICROPHONE', 'SCREENSHOT', 'TAB_MONITORING'))
);

CREATE INDEX IF NOT EXISTS idx_consent_logs_student_type
    ON consent_logs (student_id, consent_type, created_at DESC);

CREATE OR REPLACE RULE consent_logs_no_update AS
    ON UPDATE TO consent_logs DO INSTEAD NOTHING;
CREATE OR REPLACE RULE consent_logs_no_delete AS
    ON DELETE TO consent_logs DO INSTEAD NOTHING;

-- ---------------------------------------------------------------------------
-- 38: retencion de datos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_retention_policy (
    id                     UUID PRIMARY KEY,
    retention_years        INTEGER     NOT NULL DEFAULT 3,
    data_types             JSONB       NOT NULL DEFAULT '[]'::jsonb,
    retention_start_date   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active              BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_data_retention_years CHECK (retention_years > 0)
);

CREATE TABLE IF NOT EXISTS archived_responses (
    id              UUID PRIMARY KEY,
    student_id      UUID        NOT NULL,
    exam_id         UUID        NOT NULL,
    attempt_id      UUID        NOT NULL,
    response_data   JSONB       NOT NULL,
    archived_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_archived_responses_student
    ON archived_responses (student_id, archived_at DESC);

-- ---------------------------------------------------------------------------
-- 39: audit trail inmutable
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY,
    actor_id        UUID,
    actor_role      VARCHAR(20) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID,
    previous_state  JSONB,
    current_state   JSONB,
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    CONSTRAINT ck_audit_logs_role CHECK (actor_role IN (
        'PROFESSOR', 'STUDENT', 'ADMIN', 'SYSTEM'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_logs_dedup
    ON audit_logs (actor_id, action, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_time
    ON audit_logs (actor_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id);

-- El audit trail es append-only por aplicacion: no existe endpoint de
-- actualizacion/borrado. No se bloquea a nivel SQL porque el derecho al
-- olvido (spec 44) necesita pseudonimizar actor_id/ip/user_agent. Es la unica
-- mutacion permitida y la ejecuta ErasureService.

-- ---------------------------------------------------------------------------
-- 41: minimizacion de datos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_capture_policy (
    id              UUID PRIMARY KEY,
    data_type       VARCHAR(50) NOT NULL UNIQUE,
    retention_days  INTEGER     NOT NULL DEFAULT 90,
    is_mandatory    BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_data_capture_retention CHECK (retention_days > 0),
    CONSTRAINT ck_data_capture_type CHECK (data_type IN (
        'NAME', 'EMAIL', 'IP_ADDRESS', 'USER_AGENT', 'CAMERA_VIDEO',
        'MICROPHONE_AUDIO', 'SCREENSHOT', 'TAB_SWITCH', 'FOCUS_LOSS', 'COPY_PASTE'))
);

INSERT INTO data_capture_policy (id, data_type, retention_days, is_mandatory) VALUES
    (gen_random_uuid(), 'NAME',             1095, TRUE),
    (gen_random_uuid(), 'EMAIL',            1095, TRUE),
    (gen_random_uuid(), 'IP_ADDRESS',         90, FALSE),
    (gen_random_uuid(), 'USER_AGENT',         90, FALSE),
    (gen_random_uuid(), 'CAMERA_VIDEO',       30, FALSE),
    (gen_random_uuid(), 'MICROPHONE_AUDIO',   30, FALSE),
    (gen_random_uuid(), 'SCREENSHOT',         30, FALSE),
    (gen_random_uuid(), 'TAB_SWITCH',        365, TRUE),
    (gen_random_uuid(), 'FOCUS_LOSS',        365, TRUE),
    (gen_random_uuid(), 'COPY_PASTE',        365, TRUE)
ON CONFLICT (data_type) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 44: derecho al olvido
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erasure_requests (
    id              UUID PRIMARY KEY,
    student_id      UUID        NOT NULL,
    erasure_type    VARCHAR(10) NOT NULL,
    requested_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT ck_erasure_type CHECK (erasure_type IN ('HARD', 'SOFT')),
    CONSTRAINT ck_erasure_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_erasure_requests_student
    ON erasure_requests (student_id, requested_at DESC);

-- ---------------------------------------------------------------------------
-- 46: derecho de objecion
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS object_requests (
    id               UUID PRIMARY KEY,
    student_id       UUID        NOT NULL,
    object_type      VARCHAR(30) NOT NULL,
    reason           TEXT,
    requested_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status           VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    resolved_at      TIMESTAMP WITH TIME ZONE,
    resolution_note  TEXT,
    CONSTRAINT ck_object_type CHECK (object_type IN (
        'PROFILING', 'DIRECT_MARKETING', 'LEGITIMATE_INTEREST')),
    CONSTRAINT ck_object_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_object_requests_student
    ON object_requests (student_id, requested_at DESC);

-- ---------------------------------------------------------------------------
-- 47: derecho de limitacion del tratamiento
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS restriction_requests (
    id                UUID PRIMARY KEY,
    student_id        UUID        NOT NULL,
    restriction_type  VARCHAR(20) NOT NULL,
    reason            TEXT,
    requested_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status            VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
    expires_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_restriction_type CHECK (restriction_type IN (
        'PROCESSING', 'STORAGE', 'SHARING')),
    CONSTRAINT ck_restriction_status CHECK (status IN (
        'PENDING', 'ACTIVE', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_restriction_requests_student
    ON restriction_requests (student_id, status, requested_at DESC);

-- ---------------------------------------------------------------------------
-- 49: notificacion de brechas de seguridad
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_breach_notifications (
    id                  UUID PRIMARY KEY,
    student_id          UUID        NOT NULL,
    breach_type         VARCHAR(30) NOT NULL,
    description         TEXT        NOT NULL,
    recommended_action  TEXT,
    notified_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT ck_breach_type CHECK (breach_type IN (
        'DATA_LEAK', 'UNAUTHORIZED_ACCESS', 'SYSTEM_COMPROMISE')),
    CONSTRAINT ck_breach_status CHECK (status IN ('PENDING', 'NOTIFIED', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_breach_student_status
    ON data_breach_notifications (student_id, status);

-- ---------------------------------------------------------------------------
-- 50: evaluacion de impacto (EIPD/DPIA)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dpia (
    id                   UUID PRIMARY KEY,
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    risk_level           VARCHAR(10) NOT NULL,
    mitigation_measures  JSONB       NOT NULL DEFAULT '[]'::jsonb,
    reviewed_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reviewed_by          UUID,
    status               VARCHAR(15) NOT NULL DEFAULT 'DRAFT',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_dpia_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_dpia_status CHECK (status IN ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_dpia_status ON dpia (status, reviewed_at DESC);

-- ---------------------------------------------------------------------------
-- 43 / 44: borrado logico en las tablas que contienen datos del alumno.
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE exam_answers
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE sentinel_alerts
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Politica de retencion por defecto: 3 anos, activa (LOPDGDD art. 5.1.e).
INSERT INTO data_retention_policy (id, retention_years, data_types, is_active)
SELECT gen_random_uuid(), 3,
       '["NAME","EMAIL","IP_ADDRESS","USER_AGENT","CAMERA_VIDEO","MICROPHONE_AUDIO","SCREENSHOT","TAB_SWITCH","FOCUS_LOSS","COPY_PASTE"]'::jsonb,
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM data_retention_policy WHERE is_active = TRUE);
