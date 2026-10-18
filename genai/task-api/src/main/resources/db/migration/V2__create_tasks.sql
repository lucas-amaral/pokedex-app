CREATE TABLE tasks
(
    id          uuid          NOT NULL,
    owner_id    uuid          NOT NULL,
    title       varchar(100)  NOT NULL,
    description varchar(2000),
    status      varchar(11)   NOT NULL,
    due_date    date,
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    version     bigint        NOT NULL,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    -- Every task has an owner that exists; a user with tasks cannot be deleted by accident (no cascade).
    CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    -- The last line of defence if a bug or a manual edit bypasses the domain rules.
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_tasks_title CHECK (length(btrim(title)) BETWEEN 1 AND 100),
    CONSTRAINT ck_tasks_version CHECK (version >= 0)
);

-- Serve the list queries, which always filter by owner: by status, and by due date range or order.
CREATE INDEX ix_tasks_owner_status ON tasks (owner_id, status);
CREATE INDEX ix_tasks_owner_due_date ON tasks (owner_id, due_date);
