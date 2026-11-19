-- Baseline migration for review. Domain tables arrive in M1.
CREATE TABLE schema_info (
    id          INT PRIMARY KEY,
    description VARCHAR(200) NOT NULL
);
INSERT INTO schema_info (id, description) VALUES (1, 'review-service baseline');
