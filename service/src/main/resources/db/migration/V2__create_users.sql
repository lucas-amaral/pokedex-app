-- User accounts: the secondary collection for user management.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL CONSTRAINT uk_users_username UNIQUE,
    email         VARCHAR(254) NOT NULL CONSTRAINT uk_users_email UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
