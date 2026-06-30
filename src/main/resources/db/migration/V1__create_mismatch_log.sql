CREATE TABLE mismatch_log (
    id               BIGSERIAL PRIMARY KEY,
    request_id       UUID        NOT NULL,
    prompt           TEXT        NOT NULL,
    primary_output   TEXT        NOT NULL,
    candidate_output TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mismatch_log_created_at ON mismatch_log (created_at DESC);