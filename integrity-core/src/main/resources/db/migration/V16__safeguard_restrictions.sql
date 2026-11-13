-- V16: restricciones configurables de SafeGuard por examen.
--
-- El spec 19 pide exponer por examen una lista de restricciones con umbral,
-- ventana y severidad (TAB_SWITCH, FOCUS_LOSS, COPY_PASTE, DISCONNECTION) y dos
-- flags de alcance (apps/pestañas externas). La configuracion escalar existente
-- (V7) se conserva: estas columnas y la tabla nueva son aditivas.

ALTER TABLE safeguard_configuration
    ADD COLUMN IF NOT EXISTS allow_external_apps BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allow_external_tabs BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS safeguard_restriction (
    id              UUID PRIMARY KEY,
    exam_id         UUID        NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    max_occurrences INTEGER     NOT NULL DEFAULT 3,
    window_seconds  INTEGER     NOT NULL DEFAULT 300,
    severity        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_safeguard_restriction_exam_type UNIQUE (exam_id, type),
    CONSTRAINT ck_safeguard_restriction_type CHECK (type IN (
        'TAB_SWITCH', 'FOCUS_LOSS', 'COPY_PASTE', 'DISCONNECTION')),
    CONSTRAINT ck_safeguard_restriction_max CHECK (max_occurrences > 0),
    CONSTRAINT ck_safeguard_restriction_window CHECK (window_seconds > 0),
    CONSTRAINT ck_safeguard_restriction_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX IF NOT EXISTS idx_safeguard_restriction_exam ON safeguard_restriction (exam_id);
