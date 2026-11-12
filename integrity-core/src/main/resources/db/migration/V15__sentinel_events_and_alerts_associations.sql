-- V15: completa el esquema Sentinel (V8) con la asociacion examen/intento/alumno,
-- el tipo de alerta, el lifecycle (resolved) y los metadatos del evento de origen.
--
-- El spec 06 proponia crear tablas nuevas (suspicious_events/alerts) porque asumia
-- que la ultima migracion era V002. En este repositorio las tablas Sentinel ya
-- existen desde V8, asi que se extienden en lugar de duplicarlas.

-- ---------------------------------------------------------------------------
-- Eventos sospechosos
-- ---------------------------------------------------------------------------

ALTER TABLE sentinel_suspicious_events
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS exam_id UUID,
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'SENTINEL',
    ADD COLUMN IF NOT EXISTS payload TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_sentinel_events_exam ON sentinel_suspicious_events(exam_id);
CREATE INDEX IF NOT EXISTS idx_sentinel_events_event_id ON sentinel_suspicious_events(event_id);
CREATE INDEX IF NOT EXISTS idx_sentinel_events_source ON sentinel_suspicious_events(source);

-- ---------------------------------------------------------------------------
-- Alertas
-- ---------------------------------------------------------------------------

ALTER TABLE sentinel_alerts
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS alert_type VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS exam_id UUID,
    ADD COLUMN IF NOT EXISTS resolved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_sentinel_alerts_exam ON sentinel_alerts(exam_id);
CREATE INDEX IF NOT EXISTS idx_sentinel_alerts_alert_type ON sentinel_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_sentinel_alerts_resolved ON sentinel_alerts(resolved);
CREATE INDEX IF NOT EXISTS idx_sentinel_alerts_event_id ON sentinel_alerts(event_id);
