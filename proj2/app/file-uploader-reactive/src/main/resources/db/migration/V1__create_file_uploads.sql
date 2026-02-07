CREATE TABLE IF NOT EXISTS file_uploads (
    id UUID PRIMARY KEY,
    client_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_object_key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_file_uploads_client_idempotency UNIQUE (client_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_file_uploads_created_at ON file_uploads (created_at);
