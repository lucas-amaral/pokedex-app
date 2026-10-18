-- Demo accounts with published passwords (see the README): alice / wonderland123, bob / builder12345.
-- Repeatable rather than versioned, so new schema versions can still be added after it; ON CONFLICT keeps reruns safe.
INSERT INTO users (id, username, password_hash, created_at)
VALUES ('6b1f8a2e-1c3d-4e5f-8a9b-0c1d2e3f4a5b', 'alice',
        '$2y$10$lR2cQUbUsFQhwnUgRbiVb.Ui5n2ooiqDCXias0VriAKfOLdF7.dhO', '2026-10-18T00:00:00Z'),
       ('7c2a9b3f-2d4e-4f6a-9b0c-1d2e3f4a5b6c', 'bob',
        '$2y$10$6BPfwI3ty9dJooYQqO36f.DATVZmyVV.aIXmFH/rbwQGRp0LzQCaG', '2026-10-18T00:00:00Z')
ON CONFLICT (username) DO NOTHING;
