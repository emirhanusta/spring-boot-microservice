-- DEV ONLY: seeded administrator, username 'admin' / password 'admin'. Change the password (or replace this
-- seed) before exposing the service anywhere real.
INSERT INTO app_user (id, username, email, password_hash, role, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin', 'admin@readscape.local',
        '$2b$10$uGQ4WN91wDBN16/A/KZFqu9SmZ6VZAPszqCCUEsfdzXAIEFX0E93C', 'ADMIN', now(), now(), 0);
