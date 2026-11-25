-- identity-service schema. Migrations are append-only from here on.
CREATE TABLE app_user (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN'))
);
