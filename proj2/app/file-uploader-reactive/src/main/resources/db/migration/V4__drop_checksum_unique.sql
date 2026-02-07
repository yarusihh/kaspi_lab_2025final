ALTER TABLE file_uploads DROP CONSTRAINT IF EXISTS uq_file_uploads_checksum;
DROP INDEX IF EXISTS idx_file_uploads_checksum;
