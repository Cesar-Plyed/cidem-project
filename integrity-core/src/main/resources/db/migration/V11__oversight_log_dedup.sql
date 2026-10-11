-- Dedup de eventos entre módulos: SafeGuard registra la violación vía REST y,
-- desde ahora, Sentinel puede emitir una alerta por el mismo hecho (mismo
-- attemptId). La clave = attemptId + source + severity + description + occurred_at
-- (segundos) hace que la misma violación reportada dos veces solo penalice UNA
-- vez en el índice de integridad.
ALTER TABLE oversight_log_entries
    ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(120);

-- Rellena la clave para las filas existentes con la misma fórmula del backend.
UPDATE oversight_log_entries
SET dedup_key = md5(
        attempt_id::text || '|' ||
        source::text || '|' ||
        severity::text || '|' ||
        description || '|' ||
        to_char(occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS')
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_oversight_log_entries_dedup
    ON oversight_log_entries(dedup_key)
    WHERE dedup_key IS NOT NULL;
