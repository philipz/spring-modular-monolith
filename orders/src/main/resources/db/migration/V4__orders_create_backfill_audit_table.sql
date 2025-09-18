SET search_path TO orders;

CREATE TABLE IF NOT EXISTS backfill_audit
(
    id                BIGSERIAL PRIMARY KEY,
    started_at        TIMESTAMP NOT NULL,
    completed_at      TIMESTAMP,
    source_since      TIMESTAMP,
    record_limit      INTEGER,
    records_processed INTEGER DEFAULT 0,
    status            TEXT      NOT NULL,
    error_message     TEXT
);

CREATE INDEX IF NOT EXISTS idx_backfill_audit_started_at ON backfill_audit (started_at);
