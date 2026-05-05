-- V1__init.sql
-- This file creates the main audit_report table in PostgreSQL.
-- Flyway runs this automatically when the app starts.

CREATE TABLE audit_report (
                              id                BIGSERIAL PRIMARY KEY,
                              title             VARCHAR(255)        NOT NULL,
                              description       TEXT,
                              status            VARCHAR(50)         NOT NULL DEFAULT 'PENDING',
                              score             INTEGER             CHECK (score >= 0 AND score <= 100),
                              category          VARCHAR(100),
                              assigned_to       VARCHAR(150),
                              due_date          DATE,
                              ai_description    TEXT,
                              ai_recommendations TEXT,
                              is_deleted        BOOLEAN             NOT NULL DEFAULT FALSE,
                              created_by        VARCHAR(150),
                              updated_by        VARCHAR(150),
                              created_at        TIMESTAMP           NOT NULL DEFAULT NOW(),
                              updated_at        TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Index speeds up searching by status (very common query)
CREATE INDEX idx_audit_report_status   ON audit_report(status);

-- Index speeds up searching by due_date (used for overdue checks)
CREATE INDEX idx_audit_report_due_date ON audit_report(due_date);

-- Index speeds up searching by category
CREATE INDEX idx_audit_report_category ON audit_report(category);