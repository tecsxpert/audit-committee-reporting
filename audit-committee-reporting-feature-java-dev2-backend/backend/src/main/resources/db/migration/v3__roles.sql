-- V3__roles.sql
-- Creates the roles table and inserts the 3 default roles.
-- ADMIN can do everything. MANAGER can edit. VIEWER can only read.

CREATE TABLE role (
                      id   BIGSERIAL PRIMARY KEY,
                      name VARCHAR(50) UNIQUE NOT NULL
);

INSERT INTO role (name) VALUES ('ADMIN');
INSERT INTO role (name) VALUES ('MANAGER');
INSERT INTO role (name) VALUES ('VIEWER');