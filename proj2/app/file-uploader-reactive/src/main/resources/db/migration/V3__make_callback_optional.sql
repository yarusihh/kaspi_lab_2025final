ALTER TABLE file_uploads ALTER COLUMN callback_url DROP NOT NULL;
ALTER TABLE file_uploads ALTER COLUMN callback_url SET DEFAULT '';
