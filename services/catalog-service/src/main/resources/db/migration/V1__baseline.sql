-- Baseline migration for catalog. Domain tables arrive in M1.
CREATE TABLE schema_info (
    id          INT PRIMARY KEY,
    description VARCHAR(200) NOT NULL
);
INSERT INTO schema_info (id, description) VALUES (1, 'catalog-service baseline');
