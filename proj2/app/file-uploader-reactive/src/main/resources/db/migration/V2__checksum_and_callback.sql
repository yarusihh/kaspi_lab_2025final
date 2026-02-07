ALTER TABLE file_uploads ADD COLUMN IF NOT EXISTS checksum VARCHAR(128);
ALTER TABLE file_uploads ADD COLUMN IF NOT EXISTS callback_url VARCHAR(1024);

UPDATE file_uploads
SET checksum = COALESCE(checksum, idempotency_key),
    callback_url = COALESCE(callback_url, '')
WHERE checksum IS NULL OR callback_url IS NULL;

ALTER TABLE file_uploads ALTER COLUMN checksum SET NOT NULL;
ALTER TABLE file_uploads ALTER COLUMN callback_url SET NOT NULL;

ALTER TABLE file_uploads DROP CONSTRAINT IF EXISTS uq_file_uploads_client_idempotency;
ALTER TABLE file_uploads DROP COLUMN IF EXISTS client_id;
ALTER TABLE file_uploads DROP COLUMN IF EXISTS idempotency_key;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_file_uploads_checksum'
    ) THEN
        ALTER TABLE file_uploads ADD CONSTRAINT uq_file_uploads_checksum UNIQUE (checksum);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_file_uploads_checksum ON file_uploads (checksum);
