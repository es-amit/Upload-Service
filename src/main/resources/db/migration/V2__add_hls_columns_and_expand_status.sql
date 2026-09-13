ALTER TABLE upload_sessions
    ADD COLUMN hls_master_key  VARCHAR(500)  NULL,
    ADD COLUMN transcode_error VARCHAR(1000) NULL;