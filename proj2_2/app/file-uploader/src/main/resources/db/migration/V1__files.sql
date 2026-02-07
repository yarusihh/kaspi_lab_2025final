CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    upload_date TIMESTAMP NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    hash VARCHAR(128) NOT NULL
);

CREATE INDEX idx_files_uuid ON files(uuid);
CREATE INDEX idx_files_hash ON files(hash);
