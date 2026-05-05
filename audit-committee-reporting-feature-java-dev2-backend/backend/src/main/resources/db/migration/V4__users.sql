-- V4__users.sql
-- Creates the app_user table (we use app_user not "user" because
-- "user" is a reserved keyword in PostgreSQL).

CREATE TABLE app_user (
                          id           BIGSERIAL    PRIMARY KEY,
                          username     VARCHAR(100) UNIQUE NOT NULL,
                          password     VARCHAR(255) NOT NULL,        -- stored as bcrypt hash, never plain text
                          email        VARCHAR(255) UNIQUE NOT NULL,
                          role         VARCHAR(50)  NOT NULL DEFAULT 'VIEWER',
                          is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Speed up login lookup by username
CREATE INDEX idx_app_user_username ON app_user(username);

-- Insert default admin user
-- Password is "admin123" hashed with BCrypt
-- To generate a new hash: https://bcrypt-generator.com (12 rounds)
INSERT INTO app_user (username, password, email, role)
VALUES (
           'admin',
           '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
           'admin@tool27.com',
           'ADMIN'
       );

-- Insert a manager user (password: manager123)
INSERT INTO app_user (username, password, email, role)
VALUES (
           'manager',
           '$2a$12$LTfBLfOFZWuHMB7Iv0Vf4O7tM7j1TqD0ROD7z3c0K.MtEJ5b2bHwS',
           'manager@tool27.com',
           'MANAGER'
       );