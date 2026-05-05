-- V2__audit_log.sql
-- This table records every change made to any data in the system.
-- Example: if someone updates a report, we save the old and new data here.

CREATE TABLE audit_log (
                           id            BIGSERIAL PRIMARY KEY,
                           entity_type   VARCHAR(100)  NOT NULL,   -- e.g. "AuditReport"
                           entity_id     BIGINT        NOT NULL,   -- e.g. 42 (the report's ID)
                           action        VARCHAR(50)   NOT NULL,   -- CREATE, UPDATE, DELETE
                           old_value     TEXT,                     -- JSON of old data (before change)
                           new_value     TEXT,                     -- JSON of new data (after change)
                           performed_by  VARCHAR(150),             -- username who made the change
                           performed_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Composite index: speeds up "show me all changes to AuditReport #42"
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);

-- Index for finding all actions by a specific user
CREATE INDEX idx_audit_log_user ON audit_log(performed_by);