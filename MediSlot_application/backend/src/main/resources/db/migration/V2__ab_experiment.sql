CREATE TABLE ab_assignment (
    id BIGSERIAL PRIMARY KEY,
    experiment_key VARCHAR(80) NOT NULL,
    visitor_id VARCHAR(36) NOT NULL,
    variant CHAR(1) NOT NULL CHECK (variant IN ('A', 'B')),
    exposed BOOLEAN NOT NULL DEFAULT FALSE,
    exposed_at TIMESTAMPTZ,
    converted BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (experiment_key, visitor_id)
);
