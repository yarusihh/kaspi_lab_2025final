DROP INDEX IF EXISTS idx_files_hash;

ALTER TABLE files
    ADD CONSTRAINT uq_files_hash UNIQUE (hash);
