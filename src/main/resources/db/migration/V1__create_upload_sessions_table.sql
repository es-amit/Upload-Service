CREATE TABLE upload_sessions
(
    id           BINARY(16)   NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size    BIGINT       NOT NULL,
    chunk_size   BIGINT       NOT NULL,
    total_parts  INT          NOT NULL,
    object_key   VARCHAR(255) NOT NULL,
    s3_upload_id VARCHAR(255) NOT NULL,
    fingerprint  VARCHAR(255) NOT NULL,
    status       VARCHAR(255) NOT NULL,
    created_at   datetime(6)  NULL,
    updated_at   datetime(6)  NULL,
    CONSTRAINT pk_upload_sessions PRIMARY KEY (id)
);

CREATE INDEX idx_fingerprint_status ON upload_sessions (fingerprint, status);