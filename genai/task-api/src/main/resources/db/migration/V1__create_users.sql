-- The existing User model the brief assumes: just enough to own tasks and log in.
CREATE TABLE users
(
    id            uuid         NOT NULL,
    username      varchar(30)  NOT NULL,
    -- A BCrypt hash is 60 characters; the margin leaves room for an algorithm prefix later.
    password_hash varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_username_lowercase CHECK (username = lower(username))
);
